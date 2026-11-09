package edu.itc.se.shop_jpa.web;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.service.ProductService;
import edu.itc.se.shop_jpa.web.dto.ProductView;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public Page<ProductView> search(

            @RequestParam(required = false)
            String q,

            @RequestParam(required = false)
            Category category,

            @RequestParam(required = false)
            BigDecimal maxPrice,

            @RequestParam(required = false)
            Boolean inStock,

            Pageable pageable
    ) {

        return products.search(
                q,
                category,
                maxPrice,
                inStock,
                pageable
        );
    }
}