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
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer; // For B2B customers

    private double totalAmount;

    private double tax;

    private double discountAmount;

    private double grandTotal;

    private LocalDateTime generatedAt;

    // GST Fields
    private String companyGstin; // Restaurant's GSTIN
    private String customerGstin; // Customer's GSTIN (for B2B)
    
    private Double cgst; // Central GST - nullable
    private Double sgst; // State GST - nullable
    private Double igst; // Integrated GST (for inter-state) - nullable
    
    private String placeOfSupply; // State code for GST calculation
    private Boolean isInterState; // true if inter-state transaction - nullable

    // Payment tracking
    private String paymentStatus; // PENDING, PARTIAL, PAID, OVERDUE
    private Double paidAmount; // Total amount paid so far
    private Double pendingAmount; // Amount still pending

}

