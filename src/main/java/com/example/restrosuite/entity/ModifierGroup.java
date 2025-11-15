package com.example.restrosuite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "modifier_group")
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g., "Size", "Toppings", "Spice Level"

    private String description;

    @Column(nullable = false)
    private Boolean isRequired = false; // Must select at least one

    @Column(nullable = false)
    private Boolean allowMultiple = false; // Can select multiple options

    @Column(nullable = false)
    private Integer minSelection = 0; // Minimum selections required

    private Integer maxSelection; // Maximum selections allowed (null = unlimited)

    @Column(nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuModifier> modifiers = new ArrayList<>();
}

