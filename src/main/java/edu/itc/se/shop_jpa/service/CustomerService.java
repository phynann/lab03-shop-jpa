package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Customer;
import edu.itc.se.shop_jpa.repository.CustomerRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }
}