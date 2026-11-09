package edu.itc.se.shop_jpa.service;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(Long productId, int quantity) {
        super("Insufficient stock for product %d (requested %d)"
                .formatted(productId, quantity));
    }
}
