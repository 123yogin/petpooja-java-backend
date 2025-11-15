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
@Table(name = "menu_modifier")
public class MenuModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "modifier_group_id", nullable = false)
    private ModifierGroup modifierGroup;

    @Column(nullable = false)
    private String name; // e.g., "Small", "Large", "Extra Cheese", "Mild", "Spicy"

    private String description;

    @Column(nullable = false)
    private Double price = 0.0; // Additional price for this modifier

    @Column(nullable = false)
    private Boolean isActive = true;

    private Integer displayOrder; // Order in which to display modifiers
}

