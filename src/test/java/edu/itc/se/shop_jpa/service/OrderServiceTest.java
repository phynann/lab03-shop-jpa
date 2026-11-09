package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.support.TestData;
import edu.itc.se.shop_jpa.web.dto.OrderDto;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    TestData data;

    @Test
    void markPaidProducesExactlyOneUpdateWithoutSave() {
        Long id = data.newOrder();

        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().clear();

        orderService.markPaid(id);

        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
        long updates = sessionFactory.getStatistics().getEntityStatistics(Order.class.getName()).getUpdateCount();
        assertThat(updates).isEqualTo(1);
    }

    @Test
    void detachedProductChangeIsNotPersistedUntilSave() {
        Long id = products.saveAndFlush(
                new Product("DET-1", "Detached product", Money.eur("1.00"), Category.HARDWARE)
        ).getId();

        // No surrounding transaction: p is detached at once.
        Product p = products.findById(id).orElseThrow();
        p.setStock(99);

        // Reload in a new call: the detached change was never sent to the database.
        Product reloaded = products.findById(id).orElseThrow();
        assertThat(reloaded.getStock()).isZero();

        // save() of a detached entity is a merge: one SELECT + one UPDATE in the log.
        products.save(p);
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(99);
    }

    @Test
    void readOnlyTransactionNeverFlushesModifications() {
        Long id = data.newOrder();

        // DELIBERATE TRAP: viewAndModify is read-only yet modifies the managed entity.
        orderService.viewAndModify(id);

        // Nothing was flushed: the status in the database is unchanged.
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void viewReturnsOrderDto() {
        Long id = data.newOrder();

        OrderDto dto = orderService.view(id);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.status()).isEqualTo(OrderStatus.NEW);
    }
}
