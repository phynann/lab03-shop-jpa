package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reports/products")
public class ProductReportController {

    private final ProductRepository products;

    public ProductReportController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    public List<ProductReportRow> report(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String sort
    ) {
        try {
            return products.findProductReport(
                    q, category, maxPrice, minStock, tag, sort
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }
}
