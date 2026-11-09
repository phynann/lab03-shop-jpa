package edu.itc.se.shop_jpa.web.dto;

import java.math.BigDecimal;

public record ProductReportRow(
        String sku,
        String name,
        BigDecimal amount,
        Integer stock,
        Long tagCount
) {}
