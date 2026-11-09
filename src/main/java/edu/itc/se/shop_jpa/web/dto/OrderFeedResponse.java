package edu.itc.se.shop_jpa.web.dto;

import java.util.List;

public record OrderFeedResponse(
        List<OrderFeedItem> items,
        String nextCursor,
        boolean hasNext
) {}
