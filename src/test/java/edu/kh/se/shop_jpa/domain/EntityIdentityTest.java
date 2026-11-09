package edu.kh.se.shop_jpa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
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
class EntityIdentityTest {

    @Autowired
    TestEntityManager em;

    @Test
    void newProductInHashSetIsStillFoundAfterPersist() {
        Product p = new Product("BK-100", "Atlas", Money.eur("20.00"), Category.BOOKS);
        Set<Product> set = new HashSet<>();
        set.add(p);

        em.persist(p);
        em.flush();

        assertThat(p.getId()).isNotNull();
        assertThat(set.contains(p)).isTrue();
    }

    @Test
    void equalsHoldsAcrossContextsButIdentityDoesNot() {
        Product p = new Product("BK-101", "Atlas 2", Money.eur("21.00"), Category.BOOKS);
        em.persist(p);
        em.flush();
        em.clear();

        Product loaded = em.find(Product.class, p.getId());

        assertThat(loaded).isEqualTo(p);
        assertThat(loaded).isNotSameAs(p);
    }
}