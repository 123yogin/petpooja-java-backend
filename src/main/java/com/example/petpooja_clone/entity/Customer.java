package com.example.petpooja_clone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // Company name or customer name

    private String contactPerson; // Contact person name

    private String email;

    private String phone;

    private String address;

    private String city;

    private String state;

    private String pincode;

    // GST Fields
    private String gstin; // GST Identification Number

    // B2B Fields
    private Double creditLimit; // Credit limit for B2B customers
    private String paymentTerms; // e.g., "Net 30", "Net 15", "COD"
    
    private Boolean isActive; // Active/Inactive customer

}

