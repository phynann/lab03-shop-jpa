package edu.itc.se.shop_jpa.web.dto;

import edu.itc.se.shop_jpa.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        Instant placedAt,
        String customerName,
        List<OrderLineResponse> lines
) {}