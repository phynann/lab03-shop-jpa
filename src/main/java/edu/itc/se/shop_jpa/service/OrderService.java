package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderLine;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderDto;
import edu.itc.se.shop_jpa.web.dto.OrderLineResponse;
import edu.itc.se.shop_jpa.web.dto.OrderResponse;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;

    public OrderService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findOrdersByCustomerEmail(String email) {

        List<Order> result =
                orders.findWithLinesByCustomerEmail(email);

        return result.stream()
                .map(order -> {

                    List<OrderLineResponse> lines =
                            order.getLines().stream()
                                    .map(this::toLineResponse)
                                    .toList();

                    return new OrderResponse(
                            order.getId(),
                            order.getStatus(),
                            order.getPlacedAt(),
                            order.getCustomer().getName(),
                            lines
                    );
                })
                .toList();
    }

    private OrderLineResponse toLineResponse(OrderLine line) {

        return new OrderLineResponse(
                line.getProduct().getSku(),
                line.getProduct().getName(),
                line.getQuantity(),
                line.getProduct().getPrice().amount()
        );
    }

    // Mark an order as paid: managed entity, dirty checking flushes the UPDATE at commit, no save() needed.
    @Transactional
    public void markPaid(Long orderId) {
        Order order = orders.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.PAID);
    }

    // Change the status of a managed order (used for optimistic-locking demonstrations).
    @Transactional
    public void changeStatus(Long orderId, OrderStatus next) {
        Order order = orders.findById(orderId).orElseThrow();
        order.setStatus(next);
    }

    // Read-only view of an order.
    @Transactional(readOnly = true)
    public OrderDto view(Long id) {
        Order order = orders.findById(id).orElseThrow();
        return OrderDto.from(order);
    }

    /**
     * DELIBERATE TRAP: a read-only transaction that modifies a managed entity.
     * Because read-only transactions use FlushMode.MANUAL, the change is never
     * flushed at commit and is silently lost. Do not copy this pattern.
     */
    @Transactional(readOnly = true)
    public OrderDto viewAndModify(Long id) {
        Order order = orders.findById(id).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        return OrderDto.from(order);
    }
}