package edu.kh.se.shop_jpa.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Embedded
    private Address billing;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "ship_street", length = 120)),
            @AttributeOverride(name = "city", column = @Column(name = "ship_city", length = 80)),
            @AttributeOverride(name = "zip", column = @Column(name = "ship_zip", length = 10))
    })
    private Address shipping;

    protected Customer() { }

    public Customer(String name, String email, Address billing, Address shipping) {
        this.name = name;
        this.email = email;
        this.billing = billing;
        this.shipping = shipping;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Address getBilling() { return billing; }
    public Address getShipping() { return shipping; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setBilling(Address billing) { this.billing = billing; }
    public void setShipping(Address shipping) { this.shipping = shipping; }

    @Override public boolean equals(Object o) {
        return this == o || (o instanceof Customer other && id != null && id.equals(other.getId()));
    }
    @Override public int hashCode() { return Customer.class.hashCode(); }
    @Override public String toString() { return "Customer[id=%d, name=%s, email=%s]".formatted(id, name, email); }
}