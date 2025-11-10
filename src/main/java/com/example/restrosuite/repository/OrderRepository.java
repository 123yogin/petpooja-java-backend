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
    
    // Find active orders for a table (CREATED or IN_PROGRESS status)
    @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status IN ('CREATED', 'IN_PROGRESS') ORDER BY o.createdAt DESC")
    List<Order> findActiveOrdersByTableId(UUID tableId);
    
    // Find the most recent active order for a table
    @Query(value = "SELECT * FROM orders WHERE table_id = :tableId AND status IN ('CREATED', 'IN_PROGRESS') ORDER BY created_at DESC LIMIT 1", nativeQuery = true)
    java.util.Optional<Order> findLatestActiveOrderByTableId(UUID tableId);
    
    // Find all completed orders for a table that don't have bills yet
    @Query("SELECT o FROM Order o WHERE o.table.id = :tableId AND o.status = 'COMPLETED' ORDER BY o.createdAt ASC")
    List<Order> findCompletedOrdersByTableId(UUID tableId);
}

