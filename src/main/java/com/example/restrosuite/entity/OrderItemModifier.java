package com.example.restrosuite.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Links modifiers selected by customer to order items
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "order_item_modifier")
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonBackReference
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_modifier_id", nullable = false)
    @JsonIgnoreProperties({"modifierGroup", "hibernateLazyInitializer", "handler"})
    private MenuModifier menuModifier;

    @Column(nullable = false)
    private Double price; // Price at time of order (for price history)
}

