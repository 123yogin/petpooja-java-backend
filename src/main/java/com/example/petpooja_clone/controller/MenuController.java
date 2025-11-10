package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.MenuItem;
import com.example.petpooja_clone.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @GetMapping
    public List<MenuItem> getAll() {
        return menuItemRepository.findAll();
    }

    @PostMapping
    public MenuItem addItem(@RequestBody MenuItem item) {
        item.setAvailable(true);
        return menuItemRepository.save(item);
    }

    @PutMapping("/{id}")
    public MenuItem updateItem(@PathVariable UUID id, @RequestBody MenuItem item) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        existing.setName(item.getName());
        existing.setPrice(item.getPrice());
        existing.setAvailable(item.isAvailable());
        existing.setCategory(item.getCategory());
        existing.setDescription(item.getDescription());
        return menuItemRepository.save(existing);
    }

    @PutMapping("/{id}/toggle-availability")
    public MenuItem toggleAvailability(@PathVariable UUID id) {
        MenuItem existing = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        existing.setAvailable(!existing.isAvailable());
        return menuItemRepository.save(existing);
    }

    @DeleteMapping("/bulk")
    public String deleteBulk(@RequestBody List<UUID> ids) {
        menuItemRepository.deleteAllById(ids);
        return "Items deleted!";
    }

    @DeleteMapping("/{id}")
    public String deleteItem(@PathVariable UUID id) {
        menuItemRepository.deleteById(id);
        return "Menu item deleted!";
    }

}

