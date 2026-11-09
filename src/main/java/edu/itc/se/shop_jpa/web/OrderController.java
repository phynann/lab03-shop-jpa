package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orders;

    public OrderController(OrderRepository orders) {
        this.orders = orders;
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
}