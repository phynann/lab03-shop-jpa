package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.OrderStatus;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OrderFacade {

    private final OrderService orderService;

    public OrderFacade(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Retries the whole transaction; must live outside OrderService because the
     * proxy raises ObjectOptimisticLockingFailureException at commit time.
     */
    public void changeStatusWithRetry(Long id, OrderStatus next) {
        for (int attempt = 1; ; attempt++) {
            try {
                orderService.changeStatus(id, next);
                return;
            } catch (ObjectOptimisticLockingFailureException expected) {
                if (attempt == 3) {
                    throw expected;
                }
                // Small back-off, then reload the current version and retry.
                try {
                    Thread.sleep(20 * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw expected;
                }
            }
        }
    }
}