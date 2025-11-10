package com.example.restrosuite.entity;

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
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private Integer month; // 1-12

    @Column(nullable = false)
    private Integer year;

    // Earnings
    @Column(nullable = false)
    private Double basicSalary;

    private Double houseRentAllowance; // HRA
    private Double transportAllowance;
    private Double medicalAllowance;
    private Double otherAllowances;
    private Double overtimePay;
    private Double bonus;
    private Double totalEarnings; // Calculated

    // Deductions
    private Double providentFund; // PF
    private Double professionalTax;
    private Double incomeTax;
    private Double otherDeductions;
    private Double totalDeductions; // Calculated

    // Final Amount
    @Column(nullable = false)
    private Double netSalary; // totalEarnings - totalDeductions

    // Attendance Summary
    private Integer daysPresent;
    private Integer daysAbsent;
    private Integer daysLeave;
    private Integer daysOvertime;

    @Column(nullable = false)
    private LocalDate paymentDate;

    private String paymentMode; // CASH, BANK_TRANSFER, CHEQUE
    private String status; // PENDING, PAID, CANCELLED
    private String remarks;
}

