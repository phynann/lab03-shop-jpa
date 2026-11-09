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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product>,
                ProductReportRepository {

    // Derived queries

    boolean existsBySku(String sku);

    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(
            Category category
    );

    List<Product> findByTagsName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update product
            set stock = stock - :qty, version = version + 1
            where id = :id and stock >= :qty
            """,
        nativeQuery = true
    )
    int decrementStockIfAvailable(
            @Param("id") Long id,
            @Param("qty") int qty
    );

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