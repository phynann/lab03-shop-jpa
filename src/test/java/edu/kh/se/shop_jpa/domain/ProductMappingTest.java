package edu.kh.se.shop_jpa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProductMappingTest {

    @Autowired
    TestEntityManager em;

    @Test
    void moneyAndCategoryRoundTrip() {
        Product p = new Product("HW-001", "Mechanical keyboard", Money.eur("89.90"), Category.HARDWARE);
        p.setStock(5);

        em.persist(p);
        em.flush();
        em.clear();

        Product loaded = em.find(Product.class, p.getId());

        assertThat(loaded.getPrice().amount()).isEqualByComparingTo("89.90");
        assertThat(loaded.getPrice().currency()).isEqualTo("EUR");
        assertThat(loaded.getCategory()).isEqualTo(Category.HARDWARE);
        assertThat(loaded.getStock()).isEqualTo(5);
    }

    @Test
    void categoryIsStoredAsText() {
        Product p = new Product("HW-002", "USB hub", Money.eur("19.00"), Category.HARDWARE);

        em.persist(p);
        em.flush();
        em.clear();

        Object raw = em.getEntityManager()
                .createNativeQuery("select category from product where id = :id")
                .setParameter("id", p.getId())
                .getSingleResult();

        assertThat(raw).isEqualTo("HARDWARE");
    }
}