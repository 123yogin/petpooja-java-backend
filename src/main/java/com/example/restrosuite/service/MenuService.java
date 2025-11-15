package com.example.restrosuite.service;

import com.example.restrosuite.entity.MenuItem;
import com.example.restrosuite.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MenuService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public MenuItem createMenuItem(MenuItem menuItem) {
        menuItem.setAvailable(true);
        return menuItemRepository.save(menuItem);
    }

    public MenuItem updateMenuItem(UUID id, MenuItem menuItemDetails) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        
        existing.setName(menuItemDetails.getName());
        existing.setPrice(menuItemDetails.getPrice());
        existing.setAvailable(menuItemDetails.isAvailable());
        existing.setCategory(menuItemDetails.getCategory());
        existing.setDescription(menuItemDetails.getDescription());
        existing.setHsnCode(menuItemDetails.getHsnCode());
        existing.setTaxRate(menuItemDetails.getTaxRate());
        
        return menuItemRepository.save(existing);
    }

    public MenuItem toggleAvailability(UUID id) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        existing.setAvailable(!existing.isAvailable());
        return menuItemRepository.save(existing);
    }

    public void deleteMenuItem(UUID id) {
        if (!menuItemRepository.existsById(id)) {
            throw new RuntimeException("Menu item not found");
        }
        menuItemRepository.deleteById(id);
    }

    public void deleteMenuItemsBulk(List<UUID> ids) {
        menuItemRepository.deleteAllById(ids);
    }
}

