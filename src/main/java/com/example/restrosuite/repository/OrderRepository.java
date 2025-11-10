package com.example.restrosuite.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.restrosuite.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    
    List<Order> findByCreatedAtAfter(LocalDateTime date);
    
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllOrderByCreatedAtDesc();
}

