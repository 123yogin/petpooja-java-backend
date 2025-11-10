package com.example.restrosuite.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "restaurant_table")
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String tableNumber;

    private boolean occupied;

    private Integer capacity;

    private String location; // e.g., "Floor 1", "Outdoor", "VIP Section"

    @ManyToOne
    @JoinColumn(name = "outlet_id")
    private Outlet outlet; // Multi-outlet support

}

