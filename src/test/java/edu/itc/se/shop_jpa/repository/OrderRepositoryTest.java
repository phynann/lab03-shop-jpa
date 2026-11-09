
package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.*;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private ProductRepository products;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private EntityManager entityManager;

    private Customer customer;
    private Product product;

    // Create a customer with billing and shipping addresses
    private Customer createCustomer(String name, String email) {

        Address address = new Address(
                "Street 1",
                "Phnom Penh",
                "12000"
        );

        return new Customer(name, email, address, address);
    }

    // Prepare test data
    private void setupData() {

        customer = customers.save(
                createCustomer("Ada Lovelace", "ada@example.com")
        );

        product = products.save(
                new Product(
                        "KB-1",
                        "Mechanical Keyboard",
                        new Money(new BigDecimal("49.90"), "EUR"),
                        Category.HARDWARE
                )
        );

        for (int i = 0; i < 5; i++) {

            Order order = new Order(customer);

                order.setStatus(OrderStatus.PAID);

                order.setPlacedAt(
        Instant.parse("2026-09-2" + (i + 1) + "T09:00:00Z")
);
            order.addLine(product, 2);

            orders.save(order);
        }

        orders.flush();
        entityManager.clear();
    }

    @Test
    void derivedQuery_filtersByEmailAndStatus_newestFirst() {

        setupData();

        List<Order> result =
                orders.findByCustomerEmailAndStatusOrderByPlacedAtDesc(
                        "ada@example.com",
                        OrderStatus.PAID
                );

        assertEquals(5, result.size());

        assertTrue(
                result.get(0).getPlacedAt()
                        .isAfter(result.get(4).getPlacedAt())
        );
    }

    @Test
    void fetchJoin_initializesLinesAndProduct() {

        setupData();

        List<Order> result =
                orders.findWithLinesByCustomerEmail(
                        "ada@example.com"
                );

        assertFalse(result.isEmpty());

        Order order = result.get(0);

        assertTrue(Hibernate.isInitialized(order.getLines()));

        assertTrue(
                Hibernate.isInitialized(
                        order.getLines().get(0).getProduct()
                )
        );
    }

    @Test
    void plainFindById_leavesLinesLazy() {

        setupData();

        Long id = orders.findAll().get(0).getId();

        entityManager.clear();

        Order order = orders.findById(id).orElseThrow();

        assertFalse(
                Hibernate.isInitialized(order.getLines())
        );
    }

    @Test
    void nativeQuery_returnsLowStockProducts() {

        setupData();

        List<Product> result =
                products.findLowStock(10);

        assertTrue(
                result.stream().allMatch(p -> p.getStock() < 10)
        );
    }

    @Test
    void interfaceProjection_pagedByStatus() {

        setupData();

        Page<OrderSummary> result =
                orders.findByStatus(
                        OrderStatus.PAID,
                        PageRequest.of(
                                0,
                                2,
                                Sort.by("placedAt").descending()
                        )
                );

        assertEquals(2, result.getContent().size());

        assertEquals(5, result.getTotalElements());

        assertNotNull(
                result.getContent().get(0).getCustomer().getName()
        );
    }

    @Test
    void sliceFeedContinuesFromCursorWithoutPageNumbers() {
        setupData();

        Slice<OrderSummary> first = orders.findFeedSlice(
                null,
                null,
                PageRequest.of(0, 2)
        );

        assertEquals(2, first.getNumberOfElements());
        assertTrue(first.hasNext());
        OrderSummary last = first.getContent().getLast();

        Slice<OrderSummary> next = orders.findFeedSlice(
                last.getPlacedAt(),
                last.getId(),
                PageRequest.of(0, 2)
        );

        assertEquals(2, next.getNumberOfElements());
        assertTrue(next.getContent().getFirst().getPlacedAt()
                .isBefore(last.getPlacedAt()));
    }

    @Test
    void keysetWindowContinuesFromTheLastPosition() {
        setupData();

        Window<Order> first = orders.findAllByOrderByPlacedAtDescIdDesc(
                ScrollPosition.keyset(),
                Limit.of(2)
        );

        assertEquals(2, first.size());
        assertTrue(first.hasNext());

        Window<Order> next = orders.findAllByOrderByPlacedAtDescIdDesc(
                first.positionAt(first.size() - 1),
                Limit.of(2)
        );

        assertEquals(2, next.size());
        assertTrue(next.getContent().getFirst().getPlacedAt()
                .isBefore(first.getContent().getLast().getPlacedAt()));
    }
}
