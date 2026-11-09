package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.service.OrderService;
import edu.itc.se.shop_jpa.web.dto.OrderResponse;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final OrderService orders;

    public CustomerController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/{email}/orders")
    public List<OrderResponse> customerOrders(
            @PathVariable String email
    ) {

        return orders.findOrdersByCustomerEmail(email);
    }
}