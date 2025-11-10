package com.example.restrosuite.entity;

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
public class Outlet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // Outlet name

    @Column(nullable = false)
    private String code; // Unique outlet code

    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String email;

    private String gstin; // Outlet-specific GSTIN if different

    @Column(nullable = false)
    private Boolean isActive; // Active/Inactive outlet

    private String managerName; // Outlet manager name
    private String managerPhone; // Outlet manager contact
}

