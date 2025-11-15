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
    public List<ModifierGroup> getAllModifierGroups() {
        return modifierService.getAllModifierGroups();
    }

    @GetMapping("/groups/active")
    public List<ModifierGroup> getActiveModifierGroups() {
        return modifierService.getActiveModifierGroups();
    }

    @PostMapping("/groups")
    public ModifierGroup createModifierGroup(@RequestBody ModifierGroup modifierGroup) {
        return modifierService.createModifierGroup(modifierGroup);
    }

    @PutMapping("/groups/{id}")
    public ModifierGroup updateModifierGroup(@PathVariable UUID id, @RequestBody ModifierGroup modifierGroup) {
        return modifierService.updateModifierGroup(id, modifierGroup);
    }

    @DeleteMapping("/groups/{id}")
    public String deleteModifierGroup(@PathVariable UUID id) {
        modifierService.deleteModifierGroup(id);
        return "Modifier group deleted!";
    }

    // MenuModifier endpoints
    @GetMapping("/groups/{groupId}/modifiers")
    public List<MenuModifier> getModifiersByGroup(@PathVariable UUID groupId) {
        return modifierService.getModifiersByGroup(groupId);
    }

    @GetMapping("/modifiers/active")
    public List<MenuModifier> getActiveModifiers() {
        return modifierService.getActiveModifiers();
    }

    @PostMapping("/modifiers")
    public MenuModifier createMenuModifier(@RequestBody MenuModifier menuModifier) {
        return modifierService.createMenuModifier(menuModifier);
    }

    @PutMapping("/modifiers/{id}")
    public MenuModifier updateMenuModifier(@PathVariable UUID id, @RequestBody MenuModifier menuModifier) {
        return modifierService.updateMenuModifier(id, menuModifier);
    }

    @DeleteMapping("/modifiers/{id}")
    public String deleteMenuModifier(@PathVariable UUID id) {
        modifierService.deleteMenuModifier(id);
        return "Menu modifier deleted!";
    }

    // MenuItem-ModifierGroup linking
    @GetMapping("/menu-items/{menuItemId}/modifier-groups")
    public List<MenuItemModifierGroup> getModifierGroupsForMenuItem(@PathVariable UUID menuItemId) {
        return modifierService.getModifierGroupsForMenuItem(menuItemId);
    }

    @PostMapping("/menu-items/{menuItemId}/modifier-groups/{groupId}")
    public MenuItemModifierGroup linkModifierGroupToMenuItem(
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
        return modifierService.linkModifierGroupToMenuItem(menuItem, modifierGroup, displayOrder);
    }

    @DeleteMapping("/menu-items/{menuItemId}/modifier-groups/{groupId}")
    public String unlinkModifierGroupFromMenuItem(
            @PathVariable UUID menuItemId,
            @PathVariable UUID groupId) {
        modifierService.unlinkModifierGroupFromMenuItem(menuItemId, groupId);
        return "Modifier group unlinked!";
    }
}

