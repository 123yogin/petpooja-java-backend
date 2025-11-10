package com.example.petpooja_clone.repository;

import com.example.petpooja_clone.entity.Bill;
import com.example.petpooja_clone.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {
    Optional<Bill> findByOrder(Order order);
    Optional<Bill> findByOrderId(UUID orderId);
    List<Bill> findAllByOrderByGeneratedAtDesc();
}

