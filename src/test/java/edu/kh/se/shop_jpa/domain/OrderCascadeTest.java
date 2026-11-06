package edu.kh.se.shop_jpa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OrderCascadeTest {

    @Autowired
    TestEntityManager em;

    private Order newPersistedOrder() {
        Address a = new Address("1 Main St", "Paris", "75001");
        Customer c = new Customer("Ann", "ann@example.com", a, a);
        em.persist(c);

        Product p1 = new Product("BK-001", "Novel", Money.eur("10.00"), Category.BOOKS);
        Product p2 = new Product("HW-010", "Cable", Money.eur("5.50"), Category.HARDWARE);
        em.persist(p1);
        em.persist(p2);

        Order o = new Order(c);
        o.addLine(p1, 2);
        o.addLine(p2, 3);
        em.persist(o);
        em.flush();
        return o;
    }

    private long countLines(Long orderId) {
        return em.getEntityManager()
                .createQuery("select count(l) from OrderLine l where l.order.id = :id", Long.class)
                .setParameter("id", orderId)
                .getSingleResult();
    }

    @Test
    void persistingOrderCascadesToLines() {
        Long id = newPersistedOrder().getId();
        em.clear();

        Order reloaded = em.find(Order.class, id);

        assertThat(reloaded.getLines()).hasSize(2);
        assertThat(reloaded.total().amount()).isEqualByComparingTo("36.50");
        assertThat(reloaded.total().currency()).isEqualTo("EUR");
    }

    @Test
    void removingLineDeletesItOrphanRemoval() {
        Long id = newPersistedOrder().getId();
        em.clear();

        Order reloaded = em.find(Order.class, id);
        assertThat(countLines(id)).isEqualTo(2);

        reloaded.removeLine(reloaded.getLines().get(0));
        em.flush();

        assertThat(countLines(id)).isEqualTo(1);
    }

    @Test
    void removingOrderCascadesToLines() {
        Long id = newPersistedOrder().getId();
        em.clear();

        Order reloaded = em.find(Order.class, id);
        em.remove(reloaded);
        em.flush();

        assertThat(countLines(id)).isZero();
    }
}