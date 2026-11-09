package edu.itc.se.shop_jpa.web.dto;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderDto(Long id, OrderStatus status, Instant placedAt, String customerName, List<LineDto> lines) {
    public static OrderDto from(Order o) {
        return new OrderDto(o.getId(), o.getStatus(), o.getPlacedAt(), o.getCustomer().getName(),
                o.getLines().stream().map(LineDto::from).toList());
    }
}
