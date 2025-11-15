package com.example.restrosuite.controller;

import com.example.restrosuite.entity.MenuItem;
import com.example.restrosuite.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @GetMapping
    public List<MenuItem> getAll() {
        return menuService.getAllMenuItems();
    }

    @PostMapping
    public MenuItem addItem(@RequestBody MenuItem item) {
        return menuService.createMenuItem(item);
    }

    @PutMapping("/{id}")
    public MenuItem updateItem(@PathVariable UUID id, @RequestBody MenuItem item) {
        return menuService.updateMenuItem(id, item);
    }

    @PutMapping("/{id}/toggle-availability")
    public MenuItem toggleAvailability(@PathVariable UUID id) {
        return menuService.toggleAvailability(id);
    }

    @DeleteMapping("/bulk")
    public String deleteBulk(@RequestBody List<UUID> ids) {
        menuService.deleteMenuItemsBulk(ids);
        return "Items deleted!";
    }

    @DeleteMapping("/{id}")
    public String deleteItem(@PathVariable UUID id) {
        menuService.deleteMenuItem(id);
        return "Menu item deleted!";
    }

}

