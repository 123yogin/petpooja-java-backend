package com.example.restrosuite.entity;

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
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    private Long workingHours; // in minutes
    private Long overtimeHours; // in minutes

    @Column(nullable = false)
    private String status; // PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY

    private String shift; // MORNING, EVENING, NIGHT
    private String remarks; // Notes about attendance
}

