package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money_;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.domain.Product_;
import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.domain.Tag_;
import edu.itc.se.shop_jpa.web.dto.ProductReportRow;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class ProductReportRepositoryImpl implements ProductReportRepository {

    private static final Map<String, String> SORTABLE = Map.of(
            "sku", "sku",
            "name", "name",
            "amount", "amount",
            "stock", "stock",
            "category", "category"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ProductReportRow> findProductReport(
            String q,
            Category category,
            BigDecimal maxPrice,
            Integer minStock,
            String tag,
            String sort
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductReportRow> query =
                cb.createQuery(ProductReportRow.class);
        Root<Product> product = query.from(Product.class);
        List<Predicate> predicates = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.like(cb.lower(product.get(Product_.name)), pattern));
        }
        if (category != null) {
            predicates.add(cb.equal(product.get(Product_.category), category));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    product.get(Product_.price).get(Money_.amount),
                    maxPrice
            ));
        }
        if (minStock != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    product.get(Product_.stock),
                    minStock
            ));
        }
        if (tag != null && !tag.isBlank()) {
            Join<Product, Tag> tags = product.join(Product_.tags, JoinType.INNER);
            predicates.add(cb.equal(
                    cb.lower(tags.get(Tag_.name)),
                    tag.trim().toLowerCase(Locale.ROOT)
            ));
        }

        query.select(cb.construct(
                ProductReportRow.class,
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.stock),
                cb.toLong(cb.size(product.get(Product_.tags)))
        ));
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(
                product.get(Product_.id),
                product.get(Product_.sku),
                product.get(Product_.name),
                product.get(Product_.price).get(Money_.amount),
                product.get(Product_.stock)
        );

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", -1);
            String attribute = parts[0].trim();
            if (parts.length > 2 || !SORTABLE.containsKey(attribute)) {
                throw new IllegalArgumentException("Unsupported product sort: " + sort);
            }
            boolean descending = parts.length == 2
                    && "desc".equalsIgnoreCase(parts[1].trim());
            if (parts.length == 2
                    && !"asc".equalsIgnoreCase(parts[1].trim())
                    && !"desc".equalsIgnoreCase(parts[1].trim())) {
                throw new IllegalArgumentException("Unsupported sort direction: " + sort);
            }
            query.orderBy(descending
                    ? cb.desc(sortExpression(product, attribute))
                    : cb.asc(sortExpression(product, attribute)));
        } else {
            query.orderBy(cb.asc(product.get(Product_.sku)));
        }

        return entityManager.createQuery(query).getResultList();
    }

    private jakarta.persistence.criteria.Expression<?> sortExpression(
            Root<Product> product,
            String attribute
    ) {
        return switch (attribute) {
            case "amount" -> product.get(Product_.price).get(Money_.amount);
            case "sku" -> product.get(Product_.sku);
            case "name" -> product.get(Product_.name);
            case "stock" -> product.get(Product_.stock);
            case "category" -> product.get(Product_.category);
            default -> throw new IllegalArgumentException(
                    "Unsupported product sort: " + attribute
            );
        };
    }
}
