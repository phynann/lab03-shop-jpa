package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import edu.itc.se.shop_jpa.repository.ProductSpecs;
import edu.itc.se.shop_jpa.web.dto.ProductView;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional(readOnly = true)
    public Page<ProductView> search(
            String q,
            Category category,
            BigDecimal maxPrice,
            Boolean inStock,
            Pageable pageable
    ) {

        return products.findAll(
                    ProductSpecs.filter(
                            q,
                            category,
                            maxPrice,
                            inStock
                    ),
                    pageable
                )
                .map(this::toView);
    }

    private ProductView toView(Product product) {

        return new ProductView(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice().amount(),
                product.getStock()
        );
    }
}