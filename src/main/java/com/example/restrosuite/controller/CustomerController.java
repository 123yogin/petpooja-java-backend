package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Customer;
import com.example.restrosuite.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @GetMapping("/active")
    public List<Customer> getActiveCustomers() {
        return customerRepository.findByIsActiveTrue();
    }

    @GetMapping("/{id}")
    public Customer getCustomerById(@PathVariable UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
    }

    @PostMapping
    public Customer createCustomer(@RequestBody Customer customer) {
        if (customer.getIsActive() == null) {
            customer.setIsActive(true); // Default to active
        }
        if (customer.getCreditLimit() == null) {
            customer.setCreditLimit(0.0); // Default credit limit
        }
        return customerRepository.save(customer);
    }

    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable UUID id, @RequestBody Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));

        customer.setName(customerDetails.getName());
        customer.setContactPerson(customerDetails.getContactPerson());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhone(customerDetails.getPhone());
        customer.setAddress(customerDetails.getAddress());
        customer.setCity(customerDetails.getCity());
        customer.setState(customerDetails.getState());
        customer.setPincode(customerDetails.getPincode());
        customer.setGstin(customerDetails.getGstin());
        customer.setCreditLimit(customerDetails.getCreditLimit());
        customer.setPaymentTerms(customerDetails.getPaymentTerms());
        customer.setIsActive(customerDetails.getIsActive());

        return customerRepository.save(customer);
    }

    @DeleteMapping("/{id}")
    public String deleteCustomer(@PathVariable UUID id) {
        customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
        customerRepository.deleteById(id);
        return "Customer deleted successfully!";
    }
}

