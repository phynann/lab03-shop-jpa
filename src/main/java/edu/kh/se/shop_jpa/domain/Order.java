package edu.kh.se.shop_jpa.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.NEW;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected Order() { }

    public Order(Customer customer) { this.customer = customer; }

    public OrderLine addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(this, product, quantity, product.getPrice());
        lines.add(line);
        return line;
    }

    public void removeLine(OrderLine line) {
        lines.remove(line);
        line.setOrder(null);
    }

    public List<OrderLine> getLines() { return Collections.unmodifiableList(lines); }

    public Money total() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderLine l : lines) {
            sum = sum.add(l.getUnitPrice().times(l.getQuantity()).amount());
        }
        return new Money(sum, "EUR");
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public Instant getPlacedAt() { return placedAt; }
    public long getVersion() { return version; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Order other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Order.class.hashCode(); }
    @Override public String toString() { return "Order[id=%d, status=%s]".formatted(id, status); }
}