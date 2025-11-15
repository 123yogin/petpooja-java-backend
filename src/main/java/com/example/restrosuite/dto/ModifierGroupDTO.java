package com.example.restrosuite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierGroupDTO {
    private UUID id;
    private String name;
    private String description;
    private Boolean isRequired;
    private Boolean allowMultiple;
    private Integer minSelection;
    private Integer maxSelection;
    private Boolean isActive;
    private List<MenuModifierDTO> modifiers;
}

