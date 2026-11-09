package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

public record OrderLineResponse(
        String sku,
        String productName,
        int quantity,
        BigDecimal amount
) {}