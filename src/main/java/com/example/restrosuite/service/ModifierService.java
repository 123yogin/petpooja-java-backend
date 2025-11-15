package com.example.restrosuite.service;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ModifierService {

    @Autowired
    private ModifierGroupRepository modifierGroupRepository;

    @Autowired
    private MenuModifierRepository menuModifierRepository;

    @Autowired
    private MenuItemModifierGroupRepository menuItemModifierGroupRepository;

    // ModifierGroup operations
    public List<ModifierGroup> getAllModifierGroups() {
        return modifierGroupRepository.findAll();
    }

    public List<ModifierGroup> getActiveModifierGroups() {
        return modifierGroupRepository.findByIsActiveTrue();
    }

    public ModifierGroup createModifierGroup(ModifierGroup modifierGroup) {
        return modifierGroupRepository.save(modifierGroup);
    }

    public ModifierGroup updateModifierGroup(UUID id, ModifierGroup modifierGroupDetails) {
        ModifierGroup existing = modifierGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Modifier group not found"));
        
        existing.setName(modifierGroupDetails.getName());
        existing.setDescription(modifierGroupDetails.getDescription());
        existing.setIsRequired(modifierGroupDetails.getIsRequired());
        existing.setAllowMultiple(modifierGroupDetails.getAllowMultiple());
        existing.setMinSelection(modifierGroupDetails.getMinSelection());
        existing.setMaxSelection(modifierGroupDetails.getMaxSelection());
        existing.setIsActive(modifierGroupDetails.getIsActive());
        
        return modifierGroupRepository.save(existing);
    }

    public void deleteModifierGroup(UUID id) {
        modifierGroupRepository.deleteById(id);
    }

    // MenuModifier operations
    public List<MenuModifier> getModifiersByGroup(UUID groupId) {
        return menuModifierRepository.findByModifierGroupId(groupId);
    }

    public List<MenuModifier> getActiveModifiers() {
        return menuModifierRepository.findByIsActiveTrue();
    }

    public MenuModifier createMenuModifier(MenuModifier menuModifier) {
        return menuModifierRepository.save(menuModifier);
    }

    public MenuModifier updateMenuModifier(UUID id, MenuModifier menuModifierDetails) {
        MenuModifier existing = menuModifierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu modifier not found"));
        
        existing.setName(menuModifierDetails.getName());
        existing.setDescription(menuModifierDetails.getDescription());
        existing.setPrice(menuModifierDetails.getPrice());
        existing.setIsActive(menuModifierDetails.getIsActive());
        existing.setDisplayOrder(menuModifierDetails.getDisplayOrder());
        
        return menuModifierRepository.save(existing);
    }

    public void deleteMenuModifier(UUID id) {
        menuModifierRepository.deleteById(id);
    }

    // MenuItemModifierGroup operations
    public List<MenuItemModifierGroup> getModifierGroupsForMenuItem(UUID menuItemId) {
        return menuItemModifierGroupRepository.findByMenuItemId(menuItemId);
    }

    public MenuItemModifierGroup linkModifierGroupToMenuItem(MenuItem menuItem, ModifierGroup modifierGroup, Integer displayOrder) {
        MenuItemModifierGroup link = MenuItemModifierGroup.builder()
                .menuItem(menuItem)
                .modifierGroup(modifierGroup)
                .displayOrder(displayOrder)
                .build();
        return menuItemModifierGroupRepository.save(link);
    }

    public void unlinkModifierGroupFromMenuItem(UUID menuItemId, UUID modifierGroupId) {
        List<MenuItemModifierGroup> links = menuItemModifierGroupRepository.findByMenuItemId(menuItemId);
        links.stream()
                .filter(link -> link.getModifierGroup().getId().equals(modifierGroupId))
                .forEach(menuItemModifierGroupRepository::delete);
    }

    public void unlinkAllModifierGroupsFromMenuItem(UUID menuItemId) {
        menuItemModifierGroupRepository.deleteByMenuItemId(menuItemId);
    }
}

