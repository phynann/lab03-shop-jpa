package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ProductReportRepositoryTest {

    @Autowired
    private ProductRepository products;

    @Test
    void reportCombinesFiltersCountsAllTagsAndSortsByWhitelist() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product(
                "RP-" + suffix,
                "Mechanical keyboard",
                Money.eur("129.00"),
                Category.HARDWARE
        );
        product.setStock(4);
        product.tag(new Tag("sale-" + suffix));
        product.tag(new Tag("popular-" + suffix));
        products.saveAndFlush(product);

        List<ProductReportRow> rows = products.findProductReport(
                "keyboard",
                Category.HARDWARE,
                new BigDecimal("150.00"),
                1,
                "sale-" + suffix,
                "amount,desc"
        );

        assertEquals(1, rows.size());
        assertEquals("Mechanical keyboard", rows.getFirst().name());
        assertEquals(new BigDecimal("129.00"), rows.getFirst().amount());
        assertEquals(4, rows.getFirst().stock());
        assertEquals(2L, rows.getFirst().tagCount());
    }

    @Test
    void reportWithoutTagFilterIncludesUntaggedProducts() {
        String sku = "RP-" + UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product(
                sku,
                "Plain product",
                Money.eur("10.00"),
                Category.BOOKS
        );
        products.saveAndFlush(product);

        List<ProductReportRow> rows = products.findProductReport(
                null, null, null, null, null, null
        );

        ProductReportRow found = rows.stream()
                .filter(row -> sku.equals(row.sku()))
                .findFirst()
                .orElseThrow();
        assertEquals(0L, found.tagCount());
    }
}
