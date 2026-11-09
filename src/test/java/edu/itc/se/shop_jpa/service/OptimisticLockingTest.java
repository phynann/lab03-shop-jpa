package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.support.TestData;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OptimisticLockingTest {

    @Autowired
    OrderRepository orders;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    OrderFacade facade;

    @Autowired
    TestData data;

    @Test
    void secondWriterFailsWithOptimisticLock() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();
        CountDownLatch bothLoaded = new CountDownLatch(2);

        Function<OrderStatus, Callable<Throwable>> writer = target -> () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order o = orders.findById(orderId).orElseThrow();   // both read the same version
                    bothLoaded.countDown();
                    await(bothLoaded);
                    o.setStatus(target);                                // dirty → UPDATE at commit
                });
                return null;
            } catch (RuntimeException e) { return e; }
        };

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> results = pool.invokeAll(
                    List.of(writer.apply(OrderStatus.PAID), writer.apply(OrderStatus.CANCELLED)));
            List<Throwable> failures = results.stream().map(this::get).filter(Objects::nonNull).toList();

            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);

            OrderStatus winner = get(results.get(0)) == null ? OrderStatus.PAID : OrderStatus.CANCELLED;
            Order after = orders.findById(orderId).orElseThrow();
            assertThat(after.getStatus()).isEqualTo(winner);
            assertThat(after.getVersion()).isEqualTo(versionBefore + 1);
        }
    }

    @Test
    void facadeRetriesUntilBothWritersSucceed() throws Exception {
        Long orderId = data.newOrder();
        long versionBefore = orders.findById(orderId).orElseThrow().getVersion();

        // Each writer targets a different status, so the loser's retry performs
        // a real version-checked update rather than a no-op rewrite.
        Callable<Throwable> paid = attempt(() -> facade.changeStatusWithRetry(orderId, OrderStatus.PAID));
        Callable<Throwable> cancelled = attempt(() -> facade.changeStatusWithRetry(orderId, OrderStatus.CANCELLED));

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Throwable>> results = pool.invokeAll(List.of(paid, cancelled));
            List<Throwable> failures = results.stream().map(this::get).filter(Objects::nonNull).toList();

            assertThat(failures).isEmpty();
        }

        Order after = orders.findById(orderId).orElseThrow();
        assertThat(after.getVersion()).isEqualTo(versionBefore + 2);
    }

    private Callable<Throwable> attempt(Runnable r) {
        return () -> {
            try {
                r.run();
                return null;
            } catch (RuntimeException e) {
                return e;
            }
        };
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Throwable get(Future<Throwable> f) {
        try {
            return f.get();
        } catch (Exception e) {
            return e;
        }
    }
}
