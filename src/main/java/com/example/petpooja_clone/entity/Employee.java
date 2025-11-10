package com.example.petpooja_clone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;

    @Column(nullable = false)
    private String employeeId; // Unique employee ID

    private String department; // e.g., KITCHEN, SERVICE, MANAGEMENT
    private String designation; // e.g., Chef, Waiter, Manager
    private String shift; // MORNING, EVENING, NIGHT, GENERAL

    @Column(nullable = false)
    private Double basicSalary;

    private Double allowances; // HRA, Transport, etc.
    private Double deductions; // PF, Tax, etc.

    @Column(nullable = false)
    private LocalDate joinDate;

    private LocalDate dateOfBirth;
    private String gender; // MALE, FEMALE, OTHER
    private String emergencyContact;
    private String emergencyPhone;

    @Column(nullable = false)
    private Boolean isActive; // For active/inactive employees

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Link to User account if exists

    @ManyToOne
    @JoinColumn(name = "outlet_id")
    private Outlet outlet; // Multi-outlet support
}

