package com.example.restrosuite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuModifierDTO {
    private UUID id;
    private String name;
    private String description;
    private Double price;
    private Boolean isActive;
    private Integer displayOrder;
}

