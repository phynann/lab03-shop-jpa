
package edu.itc.se.shop_jpa.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(
            name = "product_seq",
            sequenceName = "product_seq",
            allocationSize = 50
    )
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Embedded
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private int stock;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE
    })
    @JoinTable(
            name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    protected Product() {
    }

    public Product(String sku, String name, Money price, Category category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = 0;
    }

    // Add a tag to this product
    public void tag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }

        tags.add(tag);
        tag.getProducts().add(this);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public Category getCategory() {
        return category;
    }

    public int getStock() {
        return stock;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(Money price) {
        this.price = price;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Product other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Product.class.hashCode();
    }

    @Override
    public String toString() {
        return "Product[id=%d, sku=%s, name=%s, category=%s, stock=%d]"
                .formatted(id, sku, name, category, stock);
    }
}
