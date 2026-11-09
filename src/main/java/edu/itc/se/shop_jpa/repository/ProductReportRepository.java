package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

import java.math.BigDecimal;
import java.util.List;

public interface ProductReportRepository {
    List<ProductReportRow> findProductReport(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort
    );
}
