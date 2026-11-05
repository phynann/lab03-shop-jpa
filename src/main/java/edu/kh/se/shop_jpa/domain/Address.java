package edu.kh.se.shop_jpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
        @Column(name = "street", length = 120) String street,
        @Column(name = "city", length = 80) String city,
        @Column(name = "zip", length = 10) String zip) { }