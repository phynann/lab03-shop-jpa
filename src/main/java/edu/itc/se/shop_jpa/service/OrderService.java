package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderLine;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderResponse;
import edu.itc.se.shop_jpa.web.dto.OrderLineResponse;

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
}