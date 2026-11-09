package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.support.QueryCounter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BatchInsertTest {
    @Autowired ProductRepository products;
    @Autowired QueryCounter counter;

    @Test
    @Transactional
    void sequenceIdsAllowProductInsertsToBeBatched() {
        counter.reset();
        products.saveAll(IntStream.range(0, 1_000)
                .mapToObj(i -> new Product("BATCH-%04d".formatted(i), "Batch " + i,
                        Money.eur("1.00"), Category.HARDWARE))
                .toList());
        products.flush();

        assertThat(counter.inserts()).isEqualTo(1_000);
        assertThat(counter.count()).isLessThan(1_000);
    }
}
