package edu.itc.se.shop_jpa.web.dto;

import edu.itc.se.shop_jpa.domain.OrderStatus;
import java.time.Instant;

public interface OrderSummary {

    Long getId();

    OrderStatus getStatus();

    Instant getPlacedAt();

    CustomerName getCustomer();

    interface CustomerName {
        String getName();
    }
}