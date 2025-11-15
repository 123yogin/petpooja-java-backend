package com.example.restrosuite.controller;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.MenuItemRepository;
import com.example.restrosuite.repository.ModifierGroupRepository;
import com.example.restrosuite.service.ModifierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/modifiers")
public class ModifierController {

    @Autowired
    private ModifierService modifierService;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    // ModifierGroup endpoints
    @GetMapping("/groups")
    public List<com.example.restrosuite.dto.ModifierGroupDTO> getAllModifierGroups() {
        return modifierService.getAllModifierGroupsDTO();
    }

    @GetMapping("/groups/active")
    public List<com.example.restrosuite.dto.ModifierGroupDTO> getActiveModifierGroups() {
        return modifierService.getActiveModifierGroupsDTO();
    }

    @PostMapping("/groups")
    public com.example.restrosuite.dto.ModifierGroupDTO createModifierGroup(@RequestBody ModifierGroup modifierGroup) {
        ModifierGroup saved = modifierService.createModifierGroup(modifierGroup);
        List<MenuModifier> modifiers = modifierService.getModifiersByGroup(saved.getId());
        return com.example.restrosuite.dto.ModifierGroupDTO.builder()
            .id(saved.getId())
            .name(saved.getName())
            .description(saved.getDescription())
            .isRequired(saved.getIsRequired())
            .allowMultiple(saved.getAllowMultiple())
            .minSelection(saved.getMinSelection())
            .maxSelection(saved.getMaxSelection())
            .isActive(saved.getIsActive())
            .modifiers(modifiers.stream().map(mod -> com.example.restrosuite.dto.MenuModifierDTO.builder()
                .id(mod.getId())
                .name(mod.getName())
                .description(mod.getDescription())
                .price(mod.getPrice())
                .isActive(mod.getIsActive())
                .displayOrder(mod.getDisplayOrder())
                .build()).collect(java.util.stream.Collectors.toList()))
            .build();
    }

    @PutMapping("/groups/{id}")
    public com.example.restrosuite.dto.ModifierGroupDTO updateModifierGroup(@PathVariable UUID id, @RequestBody ModifierGroup modifierGroup) {
        ModifierGroup updated = modifierService.updateModifierGroup(id, modifierGroup);
        List<MenuModifier> modifiers = modifierService.getModifiersByGroup(updated.getId());
        return com.example.restrosuite.dto.ModifierGroupDTO.builder()
            .id(updated.getId())
            .name(updated.getName())
            .description(updated.getDescription())
            .isRequired(updated.getIsRequired())
            .allowMultiple(updated.getAllowMultiple())
            .minSelection(updated.getMinSelection())
            .maxSelection(updated.getMaxSelection())
            .isActive(updated.getIsActive())
            .modifiers(modifiers.stream().map(mod -> com.example.restrosuite.dto.MenuModifierDTO.builder()
                .id(mod.getId())
                .name(mod.getName())
                .description(mod.getDescription())
                .price(mod.getPrice())
                .isActive(mod.getIsActive())
                .displayOrder(mod.getDisplayOrder())
                .build()).collect(java.util.stream.Collectors.toList()))
            .build();
    }

    @DeleteMapping("/groups/{id}")
    public String deleteModifierGroup(@PathVariable UUID id) {
        modifierService.deleteModifierGroup(id);
        return "Modifier group deleted!";
    }

    // MenuModifier endpoints
    @GetMapping("/groups/{groupId}/modifiers")
    public List<com.example.restrosuite.dto.MenuModifierDTO> getModifiersByGroup(@PathVariable UUID groupId) {
        return modifierService.getModifiersByGroupDTO(groupId);
    }

    @GetMapping("/modifiers/active")
    public List<com.example.restrosuite.dto.MenuModifierDTO> getActiveModifiers() {
        List<MenuModifier> modifiers = modifierService.getActiveModifiers();
        return modifiers.stream().map(mod -> com.example.restrosuite.dto.MenuModifierDTO.builder()
            .id(mod.getId())
            .name(mod.getName())
            .description(mod.getDescription())
            .price(mod.getPrice())
            .isActive(mod.getIsActive())
            .displayOrder(mod.getDisplayOrder())
            .build()).collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/modifiers")
    public com.example.restrosuite.dto.MenuModifierDTO createMenuModifier(@RequestBody MenuModifier menuModifier) {
        MenuModifier saved = modifierService.createMenuModifier(menuModifier);
        return com.example.restrosuite.dto.MenuModifierDTO.builder()
            .id(saved.getId())
            .name(saved.getName())
            .description(saved.getDescription())
            .price(saved.getPrice())
            .isActive(saved.getIsActive())
            .displayOrder(saved.getDisplayOrder())
            .build();
    }

    @PutMapping("/modifiers/{id}")
    public com.example.restrosuite.dto.MenuModifierDTO updateMenuModifier(@PathVariable UUID id, @RequestBody MenuModifier menuModifier) {
        MenuModifier updated = modifierService.updateMenuModifier(id, menuModifier);
        return com.example.restrosuite.dto.MenuModifierDTO.builder()
            .id(updated.getId())
            .name(updated.getName())
            .description(updated.getDescription())
            .price(updated.getPrice())
            .isActive(updated.getIsActive())
            .displayOrder(updated.getDisplayOrder())
            .build();
    }

    @DeleteMapping("/modifiers/{id}")
    public String deleteMenuModifier(@PathVariable UUID id) {
        modifierService.deleteMenuModifier(id);
        return "Menu modifier deleted!";
    }

    // MenuItem-ModifierGroup linking
    @GetMapping("/menu-items/{menuItemId}/modifier-groups")
    public List<com.example.restrosuite.dto.MenuItemModifierGroupDTO> getModifierGroupsForMenuItem(@PathVariable UUID menuItemId) {
        return modifierService.getModifierGroupsForMenuItemDTO(menuItemId);
    }

    @PostMapping("/menu-items/{menuItemId}/modifier-groups/{groupId}")
    public com.example.restrosuite.dto.MenuItemModifierGroupDTO linkModifierGroupToMenuItem(
            @PathVariable UUID menuItemId,
            @PathVariable UUID groupId,
            @RequestBody(required = false) Map<String, Object> request) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        ModifierGroup modifierGroup = modifierGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Modifier group not found"));
        Integer displayOrder = request != null && request.containsKey("displayOrder") 
            ? Integer.parseInt(request.get("displayOrder").toString()) 
            : null;
        MenuItemModifierGroup link = modifierService.linkModifierGroupToMenuItem(menuItem, modifierGroup, displayOrder);
        
        // Convert to DTO to avoid circular reference
        List<MenuModifier> modifiers = modifierService.getModifiersByGroup(modifierGroup.getId());
        com.example.restrosuite.dto.ModifierGroupDTO groupDTO = com.example.restrosuite.dto.ModifierGroupDTO.builder()
            .id(modifierGroup.getId())
            .name(modifierGroup.getName())
            .description(modifierGroup.getDescription())
            .isRequired(modifierGroup.getIsRequired())
            .allowMultiple(modifierGroup.getAllowMultiple())
            .minSelection(modifierGroup.getMinSelection())
            .maxSelection(modifierGroup.getMaxSelection())
            .isActive(modifierGroup.getIsActive())
            .modifiers(modifiers.stream().map(mod -> com.example.restrosuite.dto.MenuModifierDTO.builder()
                .id(mod.getId())
                .name(mod.getName())
                .description(mod.getDescription())
                .price(mod.getPrice())
                .isActive(mod.getIsActive())
                .displayOrder(mod.getDisplayOrder())
                .build()).collect(java.util.stream.Collectors.toList()))
            .build();
        
        return com.example.restrosuite.dto.MenuItemModifierGroupDTO.builder()
            .id(link.getId())
            .modifierGroup(groupDTO)
            .displayOrder(link.getDisplayOrder())
            .build();
    }

    @DeleteMapping("/menu-items/{menuItemId}/modifier-groups/{groupId}")
    public String unlinkModifierGroupFromMenuItem(
            @PathVariable UUID menuItemId,
            @PathVariable UUID groupId) {
        modifierService.unlinkModifierGroupFromMenuItem(menuItemId, groupId);
        return "Modifier group unlinked!";
    }
}

