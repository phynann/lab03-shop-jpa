package edu.kh.se.shop_jpa.domain;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_seq")
    @SequenceGenerator(name = "tag_seq", sequenceName = "tag_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Product> products = new HashSet<>();

    protected Tag() { }

    public Tag(String name) { this.name = name; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Set<Product> getProducts() { return products; }
    public void setName(String name) { this.name = name; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Tag other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Tag.class.hashCode(); }
    @Override public String toString() { return "Tag[id=%d, name=%s]".formatted(id, name); }
}