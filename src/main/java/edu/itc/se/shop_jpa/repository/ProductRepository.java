package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Category;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    // Derived queries

    boolean existsBySku(String sku);

    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(
            Category category
    );

    List<Product> findByTagsName(String name);

    // Native SQL query

    @Query(
        value = """
            SELECT p.*
            FROM product p
            WHERE p.stock < :min
            ORDER BY p.stock
            """,
        nativeQuery = true
    )
    List<Product> findLowStock(@Param("min") int min);

    /*
     * Record projection:
     * See the explanation below about the embedded Money field.
     */
}