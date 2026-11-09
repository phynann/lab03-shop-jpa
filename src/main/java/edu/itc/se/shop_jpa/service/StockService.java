package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StockService {

    private static final int MAX_OPTIMISTIC_RETRIES = 100;

    private final ProductRepository products;
    private final TransactionTemplate transactionTemplate;

    public StockService(
            ProductRepository products,
            PlatformTransactionManager transactionManager
    ) {
        this.products = products;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        requirePositiveQuantity(quantity);
        Product product = products.findForUpdate(productId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Product not found: " + productId
                ));
        ensureAvailable(product, quantity, productId);
        product.setStock(product.getStock() - quantity);
    }

    public void reserveOptimistic(Long productId, int quantity) {
        requirePositiveQuantity(quantity);
        for (int attempt = 0; attempt < MAX_OPTIMISTIC_RETRIES; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Product product = products.findById(productId)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Product not found: " + productId
                            ));
                    ensureAvailable(product, quantity, productId);
                    product.setStock(product.getStock() - quantity);
                });
                return;
            } catch (ObjectOptimisticLockingFailureException
                     | OptimisticLockException exception) {
                if (attempt + 1 == MAX_OPTIMISTIC_RETRIES) {
                    throw exception;
                }
            }
        }
        throw new IllegalStateException("Optimistic reservation retry limit reached");
    }

    @Transactional
    public void reserveAtomic(Long productId, int quantity) {
        requirePositiveQuantity(quantity);
        if (products.decrementStockIfAvailable(productId, quantity) == 0) {
            if (!products.existsById(productId)) {
                throw new EntityNotFoundException("Product not found: " + productId);
            }
            throw new OutOfStockException(productId, quantity);
        }
    }

    private void ensureAvailable(Product product, int quantity, Long productId) {
        if (product.getStock() < quantity) {
            throw new OutOfStockException(productId, quantity);
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }
    }
}
