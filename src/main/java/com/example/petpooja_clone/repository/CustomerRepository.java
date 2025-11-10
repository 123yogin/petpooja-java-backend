package com.example.petpooja_clone.repository;

import com.example.petpooja_clone.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    Optional<Customer> findByGstin(String gstin);
    
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    List<Customer> findByIsActiveTrue();
}

