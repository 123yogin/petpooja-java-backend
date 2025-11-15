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
public class MenuItemModifierGroupDTO {
    private UUID id;
    private ModifierGroupDTO modifierGroup;
    private Integer displayOrder;
}

