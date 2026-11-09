package edu.itc.se.shop_jpa.web.dto;

import edu.itc.se.shop_jpa.domain.OrderLine;
import java.math.BigDecimal;

public record LineDto(Long productId, String sku, String name, int quantity, BigDecimal unitPrice) {
    public static LineDto from(OrderLine l) {
        var p = l.getProduct();
        return new LineDto(p.getId(), p.getSku(), p.getName(), l.getQuantity(), p.getPrice().amount());
    }
}
