package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Bill;
import com.example.restrosuite.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {
    Optional<Bill> findByOrder(Order order);
    Optional<Bill> findByOrderId(UUID orderId);
    List<Bill> findAllByOrderByGeneratedAtDesc();
    
    // Find the most recent bill for a table (by checking orders linked to bills)
    @Query("SELECT b FROM Bill b WHERE b.order.table.id = :tableId ORDER BY b.generatedAt DESC")
    List<Bill> findBillsByTableId(UUID tableId);
}

