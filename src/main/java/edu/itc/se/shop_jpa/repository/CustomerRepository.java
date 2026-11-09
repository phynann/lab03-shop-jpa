package edu.itc.se.shop_jpa.repository;

import edu.itc.se.shop_jpa.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}