package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.service.OrderFeedService;
import edu.itc.se.shop_jpa.web.dto.OrderFeedResponse;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orders;
    private final OrderFeedService feeds;

    public OrderController(OrderRepository orders, OrderFeedService feeds) {
        this.orders = orders;
        this.feeds = feeds;
    }

    @GetMapping
    public Page<OrderSummary> list(
            @RequestParam OrderStatus status,

            @PageableDefault(
                    size = 20,
                    sort = "placedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        return orders.findByStatus(status, pageable);
    }

    @GetMapping("/feed")
    public OrderFeedResponse feed(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String after
    ) {
        try {
            return feeds.slice(size, after);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    @GetMapping("/feed/keyset")
    public OrderFeedResponse keysetFeed(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String after
    ) {
        try {
            return feeds.keyset(size, after);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }
}