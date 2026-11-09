package edu.itc.se.shop_jpa.web.dto;

import edu.itc.se.shop_jpa.domain.OrderStatus;

import java.time.Instant;

public record OrderFeedItem(
        Long id,
        OrderStatus status,
        Instant placedAt,
        String customerName
) {}
