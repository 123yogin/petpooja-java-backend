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
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo; // User assigned to complete this task

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy; // User who created this task

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    private String priority; // LOW, MEDIUM, HIGH

    private LocalDateTime dueDate;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private String notes; // Additional notes or validation comments

}

