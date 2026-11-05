package edu.kh.se.shop_jpa.domain;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(name = "product_seq", sequenceName = "product_seq", allocationSize = 50)
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

    @ManyToMany
    @JoinTable(name = "product_tag",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    protected Product() { }

    public Product(String sku, String name, Money price, Category category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = 0;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public Money getPrice() { return price; }
    public Category getCategory() { return category; }
    public int getStock() { return stock; }
    public Set<Tag> getTags() { return tags; }

    public void setName(String name) { this.name = name; }
    public void setPrice(Money price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
}