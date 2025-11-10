package com.example.petpooja_clone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String leaveType; // SICK, CASUAL, EARNED, UNPAID, MATERNITY, PATERNITY

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer numberOfDays; // Calculated

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    private String reason; // Reason for leave
    private String remarks; // Additional notes

    @ManyToOne
    @JoinColumn(name = "approved_by_id")
    private User approvedBy; // Manager/Admin who approved

    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;
    private String rejectionReason; // If rejected
}

