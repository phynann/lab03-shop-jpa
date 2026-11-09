package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StockReservationTest {

    private static final Logger log =
            LoggerFactory.getLogger(StockReservationTest.class);

    @Autowired
    private StockService stock;

    @Autowired
    private ProductRepository products;

    @Test
    void pessimisticReservationsNeverOversell() throws Exception {
        verifyConcurrentReservations(
                "pessimistic",
                stock::reserve
        );
    }

    @Test
    void optimisticReservationsNeverOversell() throws Exception {
        verifyConcurrentReservations(
                "optimistic",
                stock::reserveOptimistic
        );
    }

    @Test
    void atomicUpdateReservationsNeverOversell() throws Exception {
        verifyConcurrentReservations(
                "atomic update",
                stock::reserveAtomic
        );
    }

    private void verifyConcurrentReservations(
            String strategy,
            BiConsumer<Long, Integer> reserve
    ) throws Exception {
        Product product = new Product(
                "RSV-" + UUID.randomUUID().toString().substring(0, 8),
                "Reservation test product",
                Money.eur("1.00"),
                Category.HARDWARE
        );
        product.setStock(5);
        Long productId = products.saveAndFlush(product).getId();
        int threadCount = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        long startedAt = System.nanoTime();
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        reserve.accept(productId, 1);
                        return true;
                    } catch (OutOfStockException exception) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    succeeded++;
                }
            }
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

            assertEquals(5, succeeded);
            assertEquals(0, products.findById(productId).orElseThrow().getStock());
            log.info("{}: {} ok / {} out-of-stock, stock=0, {} threads in {} ms",
                    strategy, succeeded, threadCount - succeeded, threadCount,
                    elapsedMillis);
        } finally {
            executor.shutdownNow();
        }
    }
}
