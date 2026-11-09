package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reporting queries used to demonstrate and compare association fetching strategies. */
@Service
public class ReportService {
    private final OrderRepository orders;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrder() {
        return countLines(orders.findByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderJoinFetch() {
        return countLines(orders.findByStatusWithLines(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderEntityGraph() {
        return countLines(orders.findWithGraphByStatus(OrderStatus.PAID));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderBatched() {
        return countLines(orders.findByStatus(OrderStatus.PAID));
    }

    /** Product names are deliberately read in the same transaction as the report mapping. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> productNamesPerPaidOrder() {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Order order : orders.findByStatus(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().stream()
                    .map(line -> line.getProduct().getName())
                    .toList());
        }
        return result;
    }

    private Map<Long, Integer> countLines(List<Order> result) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (Order order : result) {
            counts.put(order.getId(), order.getLines().size());
        }
        return counts;
    }
}
