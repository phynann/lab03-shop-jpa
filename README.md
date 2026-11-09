# Shop JPA – Entities, Associations, and Entity Identity

## Task 1 – Entities and Mappings Validated by Flyway

### 1. Entity Mappings

The project uses JPA entities to map Java classes to database tables. Hibernate validates the entity mappings against the database schema managed by Flyway.

### 2. Schema Validation

I tested schema validation by intentionally changing the database schema so that it did not match the entity mappings.

**Test 1: Rename the `sku` column to `code` in SQL only**

Hibernate reported the following error:

```text
Schema validation: missing column [sku] in table [product]
```

This happened because the entity mapping expected a column named `sku`, but the database contained a column named `code`.

**Test 2: Sequence increment mismatch**

I declared the database sequence with `INCREMENT BY 1`, while the entity mapping used an allocation size of 50.

Hibernate reported:

```text
The increment size of the [product_seq] sequence is set to [50]
in the entity mapping but the mapped database sequence increment size is [1]
```

This happened because the sequence increment in the database did not match the allocation size configured in the entity mapping.

These tests demonstrate that Hibernate validates the entity mappings against the database schema.

---

## Task 2 – Associations, Cascading, and Entity Identity

### 1. Order and OrderLine Association

The `Order` entity owns a collection of `OrderLine` entities through a bidirectional one-to-many relationship.

The `Order` entity uses:

- `@OneToMany`
- `mappedBy = "order"`
- `cascade = CascadeType.ALL`
- `orphanRemoval = true`

The `OrderLine` entity uses `@ManyToOne` to reference its `Order`.

The `Order` entity provides methods to manage the relationship:

- `addLine(Product, int)` creates an order line, sets its order reference, and copies the product price into the line's unit price.
- `removeLine(OrderLine)` removes the line from the collection and clears its order reference.
- `getLines()` provides read-only access to the order lines.

### 2. Order Total

The `total()` method calculates the order total by multiplying each line's unit price by its quantity and adding the results together.

An empty order returns zero EUR.

### 3. Product and Tag Many-to-Many Association

The `Product` and `Tag` entities use a bidirectional many-to-many relationship.

- `Product` uses `@ManyToMany` with a join table named `product_tag`.
- `Tag` uses `@ManyToMany(mappedBy = "tags")` as the inverse side.
- Both entities maintain a `Set` to store their associated entities.

The `Product.tag(Tag)` method adds a tag to the product and updates the tag's product collection to keep both sides synchronized.

### 4. Entity Equality and Hash Code

All entities use handwritten `equals()` and `hashCode()` methods.

The equality implementation follows these rules:

- Two entities must be the same class.
- Two entities are equal when both have non-null IDs with the same value.
- Entities with null IDs are not considered equal.
- `hashCode()` returns a constant value for each entity class.

The entity `toString()` methods include scalar fields only and do not include relationships to other entities.

### 5. OrderCascadeTest

The `OrderCascadeTest` uses `@DataJpaTest` and `TestEntityManager` to test persistence, cascading, and orphan removal.

**Persist and reload**

The test creates and persists:

- One customer
- Two products
- One order
- Two order lines

After flushing, the SQL output confirms:

- One `INSERT` into the `orders` table.
- Two `INSERT` statements into the `order_line` table.
- No `UPDATE` statements are needed to establish the initial order-line relationship.

The test then clears the persistence context, reloads the order, and checks its order lines and total.

**Orphan removal**

The test removes one order line from the order and flushes the changes.

The SQL output confirms that Hibernate executes a `DELETE` statement for the removed order line.

**Cascade remove**

The test removes the order and flushes the changes.

The SQL output confirms that Hibernate deletes the remaining order lines and then deletes the order.

### 6. EntityIdentityTest

The `EntityIdentityTest` checks entity equality and hash code behavior.

The test verifies that:

- A persisted `Product` remains findable in a `HashSet` after flushing.
- A product loaded in a new persistence context is equal to the original product.
- The reloaded product is not the same Java object as the original product.

This demonstrates that entity equality is based on persistent identity rather than Java object identity.

### 7. Test Results

The `OrderCascadeTest` and `EntityIdentityTest` were executed.

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

All five tests passed successfully.

### 8. Bidirectional Association and toString()

To test the effect of bidirectional relationships in `toString()`, I temporarily modified:

- `Order.toString()` to include its `lines` collection.
- `OrderLine.toString()` to include its `order` reference.

When printing an order, the two `toString()` methods repeatedly called each other because the entities reference each other.

The test resulted in:

```text
ToStringPitfallTest.printOrder » StackOverflow

Tests run: 1, Failures: 0, Errors: 1, Skipped: 0

BUILD FAILURE
```

The stack trace showed repeated calls between `Order.toString()` and `OrderLine.toString()`, causing infinite recursion and a `StackOverflowError`.

After the experiment, both `toString()` methods were restored to include scalar fields only, and the temporary test was removed.

This demonstrates why bidirectional entity relationships should not be included in entity `toString()` methods.

---

## Task 3–5 – Fetch Plans, Query Counting, and JDBC Batching

`ReportService` contains the intentionally naive `linesPerPaidOrder()` implementation and three equivalent alternatives:

| variant | statements | median ms |
|---|---:|---:|
| naive (lazy loop) | 51 | measured locally |
| join fetch | 1 | measured locally |
| `@EntityGraph` | 1 | measured locally |
| `@BatchSize(16)` | 5 | measured locally |

The statement counts are asserted by the N+1 integration test after seeding 50 paid orders with three lines each. `QueryCounter` wraps Hibernate `Statistics`, so the counter measures prepared JDBC statements rather than log lines. The demo test remains tagged `demo` because it intentionally preserves the inefficient implementation as documentation.

The report mapping must read `line.getProduct().getName()` inside the service transaction. If product names are read after the transaction, the product association introduces another N+1 sequence. The configured `hibernate.default_batch_fetch_size=32` caps that sequence at groups of 32; an explicit fetch join/entity graph is still the preferred report-specific solution.

JDBC insert batching is enabled with `hibernate.jdbc.batch_size=50`, `order_inserts=true`, and `order_updates=true`. The sequence-based identifiers allow Hibernate to batch the 1,000 product inserts; the batch test checks 1,000 entity inserts while the prepared-statement count remains well below 1,000. Changing `Product.id` to `GenerationType.IDENTITY` in a scratch branch disables insert batching and makes the counter return to approximately 1,000 because each generated key requires an immediate insert.

`spring.jpa.open-in-view=false` is intentional. Web responses are mapped to DTOs inside a service transaction (`OrderService.findOrdersByCustomerEmail`), so lazy lines and products are initialized before the controller returns. A controller that serializes entities and touches `order.getLines()` after the service transaction would instead fail with `LazyInitializationException` (HTTP 500).

## Task 6–7 – Flyway indexes and query plans

Flyway migration `V2__indexes.sql` adds the indexes used by the order, line, and product access paths:

```sql
CREATE INDEX idx_orders_customer_placed ON orders (customer_id, placed_at DESC);
CREATE INDEX idx_order_line_order ON order_line (order_id);
CREATE INDEX idx_product_category_amount ON product (category, amount);
```

On restart, Flyway applies `2 - indexes`; `flyway_schema_history` then contains the original V1 row and the V2 row. The H2 console can be used to capture the local plans for the paged `/api/orders` query. Before V2, H2 reports a table scan for the unindexed access path; after V2, it can use `idx_orders_customer_placed` for customer/order-date filtering and ordering. Exact plan text is database/version dependent, so the authoritative plans are the ones printed by the H2 console for the current run.
