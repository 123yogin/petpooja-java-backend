package com.example.petpooja_clone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer; // For B2B customers

    @Column(nullable = false)
    private Double amount; // Payment amount

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private String paymentMode; // CASH, BANK_TRANSFER, CHEQUE, CARD, UPI

    private String transactionId; // For bank transfers, UPI, etc.
    private String chequeNumber; // For cheque payments
    private String bankName; // For cheque/bank transfers

    private String status; // COMPLETED, PENDING, FAILED, REFUNDED

    private String remarks; // Additional notes
}

