package com.example.petpooja_clone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String category;

    private double price;

    private boolean available;

    @Column(length = 1000)
    private String description;

    // GST Fields
    @Column(name = "hsn_code")
    private String hsnCode; // HSN code for GST
    
    @Column(name = "tax_rate")
    private Double taxRate; // GST tax rate (e.g., 5.0 for 5%, 18.0 for 18%) - nullable for existing items

}

