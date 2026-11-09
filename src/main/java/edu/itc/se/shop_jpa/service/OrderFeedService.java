package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderFeedItem;
import edu.itc.se.shop_jpa.web.dto.OrderFeedResponse;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class OrderFeedService {

    private final OrderRepository orders;

    public OrderFeedService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse slice(int size, String cursor) {
        validateSize(size);
        Cursor decoded = decode(cursor);
        Slice<OrderSummary> slice = orders.findFeedSlice(
                decoded == null ? null : decoded.placedAt(),
                decoded == null ? null : decoded.id(),
                PageRequest.of(0, size)
        );
        List<OrderFeedItem> items = slice.getContent().stream()
                .map(item -> new OrderFeedItem(
                        item.getId(),
                        item.getStatus(),
                        item.getPlacedAt(),
                        item.getCustomer().getName()
                ))
                .toList();
        return new OrderFeedResponse(
                items,
                slice.hasNext() && !items.isEmpty()
                        ? encode(items.getLast().placedAt(), items.getLast().id())
                        : null,
                slice.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse keyset(int size, String cursor) {
        validateSize(size);
        Cursor decoded = decode(cursor);
        ScrollPosition position = decoded == null
                ? ScrollPosition.keyset()
                : ScrollPosition.forward(Map.of(
                        "placedAt", decoded.placedAt(),
                        "id", decoded.id()
                ));
        Window<Order> window = orders.findAllByOrderByPlacedAtDescIdDesc(
                position,
                Limit.of(size)
        );
        List<OrderFeedItem> items = window.getContent().stream()
                .map(this::toFeedItem)
                .toList();
        return new OrderFeedResponse(
                items,
                window.hasNext() && !items.isEmpty()
                        ? encode(items.getLast().placedAt(), items.getLast().id())
                        : null,
                window.hasNext()
        );
    }

    private OrderFeedItem toFeedItem(Order order) {
        return new OrderFeedItem(
                order.getId(),
                order.getStatus(),
                order.getPlacedAt(),
                order.getCustomer().getName()
        );
    }

    private String encode(Instant placedAt, Long id) {
        return placedAt + "," + id;
    }

    private Cursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cursor must contain placedAt,id");
        }
        try {
            return new Cursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid order feed cursor", exception);
        }
    }

    private void validateSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Feed size must be between 1 and 100");
        }
    }

    private record Cursor(Instant placedAt, Long id) {}
}
