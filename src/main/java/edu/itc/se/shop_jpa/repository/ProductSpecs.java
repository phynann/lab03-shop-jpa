package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecs {

    private ProductSpecs() {}

    // Search by product name
    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) ->
                cb.like(
                    cb.lower(root.get("name")),
                    "%" + q.toLowerCase() + "%"
                );
    }

    // Search by category
    public static Specification<Product> inCategory(Category category) {
        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }

    // Search by maximum price
    public static Specification<Product> cheaperThan(BigDecimal max) {
        return (root, query, cb) ->
                cb.le(
                    root.<Money>get("price").get("amount"),
                    max
                );
    }

    // Products that have stock
    public static Specification<Product> inStock() {
        return (root, query, cb) ->
                cb.greaterThan(root.get("stock"), 0);
    }

    // Combine optional filters
    public static Specification<Product> filter(
            String q,
            Category category,
            BigDecimal maxPrice,
            Boolean inStock
    ) {

        return Specification.allOf(
                q == null || q.isBlank()
                        ? null : nameContains(q),

                category == null
                        ? null : inCategory(category),

                maxPrice == null
                        ? null : cheaperThan(maxPrice),

                inStock == null
                        ? null : inStock
                            ? ProductSpecs.inStock()
                            : (root, query, cb) ->
                                cb.equal(root.get("stock"), 0)
        );
    }
}