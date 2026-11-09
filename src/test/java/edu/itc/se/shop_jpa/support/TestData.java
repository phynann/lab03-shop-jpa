package edu.itc.se.shop_jpa.support;

import edu.itc.se.shop_jpa.domain.*;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class TestData {
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public TestData(CustomerRepository customers, ProductRepository products, OrderRepository orders) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Transactional
    public void paidOrders(int orderCount, int linesPerOrder) {
        if (orders.countByStatus(OrderStatus.PAID) >= orderCount) return;

        Customer customer = customers.findAll().stream().findFirst()
                .orElseGet(() -> customers.save(new Customer(
                        "Query Test Customer", "query-test@example.com",
                        new Address("1 Test Street", "Phnom Penh", "12000"),
                        new Address("1 Test Street", "Phnom Penh", "12000"))));

        List<Product> catalog = products.findAll();
        while (catalog.size() < linesPerOrder) {
            int n = catalog.size();
            catalog.add(products.save(new Product(
                    "QUERY-%03d".formatted(n), "Query Product " + n,
                    Money.eur("10.00"), Category.HARDWARE)));
        }

        long existing = orders.countByStatus(OrderStatus.PAID);
        for (long i = existing; i < orderCount; i++) {
            Order order = new Order(customer);
            order.setStatus(OrderStatus.PAID);
            for (int line = 0; line < linesPerOrder; line++) {
                order.addLine(catalog.get(line), 1);
            }
            orders.save(order);
        }
        orders.flush();
    }

    @Transactional
    public Long newOrder() {
        Customer customer = customers.findAll().stream().findFirst()
                .orElseGet(() -> customers.save(new Customer(
                        "Shop Customer", "shop@example.com",
                        new Address("1 Main Street", "Phnom Penh", "12000"),
                        new Address("1 Main Street", "Phnom Penh", "12000"))));
        Product product = products.save(new Product(
                "ORD-" + UUID.randomUUID().toString().substring(0, 8),
                "Order product", Money.eur("10.00"), Category.HARDWARE));
        Order order = new Order(customer);
        order.setStatus(OrderStatus.NEW);
        order.addLine(product, 1);
        return orders.save(order).getId();
    }
}
