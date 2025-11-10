package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBillId(UUID billId);
    List<Payment> findByCustomerId(UUID customerId);
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    List<Payment> findByStatus(String status);
    List<Payment> findByPaymentMode(String paymentMode);
}

