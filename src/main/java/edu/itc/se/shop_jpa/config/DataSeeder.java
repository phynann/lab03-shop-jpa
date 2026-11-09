
package edu.itc.se.shop_jpa.config;

import edu.itc.se.shop_jpa.domain.*;
import edu.itc.se.shop_jpa.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderRepository orders;

    public DataSeeder(
            CustomerRepository customers,
            ProductRepository products,
            OrderRepository orders
    ) {
        this.customers = customers;
        this.products = products;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // Prevent duplicate data
        if (customers.count() > 0) {
            System.out.println("Seed data already exists. Skipping...");
            return;
        }

        System.out.println("Starting database seeding...");

        seedCustomers();

        List<Product> productList = seedProducts();

        seedOrders(productList);

        System.out.println("Database seeding completed!");
    }

    // 1. Seed customers
    private void seedCustomers() {

        customers.saveAll(List.of(
                createCustomer("Ada Lovelace", "ada@example.com"),
                createCustomer("Grace Hopper", "grace@example.com"),
                createCustomer("Alan Turing", "alan@example.com")
        ));
    }

private Customer createCustomer(String name, String email) {

    Address address = new Address(
        "Street 1",
        "Phnom Penh",
        "12000"
    );

    return new Customer(name, email, address, address);
}
    // 2. Seed products
    private List<Product> seedProducts() {

        String[][] data = {
                {"KB-1", "Mechanical Keyboard", "49.90", "HARDWARE"},
                {"KB-7", "Gaming Keyboard", "59.90", "HARDWARE"},
                {"MS-1", "Wireless Mouse", "25.00", "HARDWARE"},
                {"HW-002", "USB Hub", "19.00", "HARDWARE"},
                {"BK-100", "Atlas", "20.00", "BOOKS"},
                {"BK-101", "Java Programming", "35.00", "BOOKS"},
                {"FD-001", "Coffee", "10.00", "FOOD"},
                {"FD-002", "Chocolate", "5.00", "FOOD"},
                {"TY-001", "Toy Car", "15.00", "TOYS"},
                {"TY-002", "Building Blocks", "30.00", "TOYS"},
                {"HW-003", "Monitor", "150.00", "HARDWARE"},
                {"HW-004", "Webcam", "45.00", "HARDWARE"}
        };

        String[] tagNames = {
                "gaming", "office", "wireless", "accessory",
                "education", "programming", "drink", "snack",
                "vehicle", "creative", "display", "video"
        };

        List<Product> result = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {

            Product product = new Product(
                    data[i][0],
                    data[i][1],
                    new Money(
                            new BigDecimal(data[i][2]),
                            "EUR"
                    ),
                    Category.valueOf(data[i][3])
            );

            product.tag(new Tag(tagNames[i]));

            result.add(product);
        }

        return products.saveAll(result);
    }

    // 3. Seed orders
    private void seedOrders(List<Product> productList) {

        List<Customer> customerList = customers.findAll();

        if (customerList.isEmpty() || productList.isEmpty()) {
            return;
        }

        OrderStatus[] statuses = OrderStatus.values();

        for (int i = 0; i < 30; i++) {

            Customer customer =
                    customerList.get(i % customerList.size());

            // Use the actual Order constructor
            Order order = new Order(customer);

            order.setStatus(statuses[i % statuses.length]);

            order.setPlacedAt(
                    Instant.now().minus(i, ChronoUnit.DAYS)
            );

            // Each order has 2-4 lines
            int numberOfLines = 2 + (i % 3);

            for (int j = 0; j < numberOfLines; j++) {

                Product product =
                        productList.get(
                                (i + j) % productList.size()
                        );

                int quantity = 1 + ((i + j) % 3);

                // Order creates the OrderLine internally
                order.addLine(product, quantity);
            }

            orders.save(order);
        }
    }
}
