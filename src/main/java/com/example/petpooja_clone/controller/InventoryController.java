package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Ingredient;
import com.example.petpooja_clone.entity.MenuIngredient;
import com.example.petpooja_clone.entity.MenuItem;
import com.example.petpooja_clone.repository.IngredientRepository;
import com.example.petpooja_clone.repository.MenuIngredientRepository;
import com.example.petpooja_clone.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private MenuIngredientRepository menuIngredientRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @GetMapping("/ingredients")
    public List<Ingredient> getIngredients() {
        return ingredientRepository.findAll();
    }

    @PostMapping("/ingredients")
    public Ingredient addIngredient(@RequestBody Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }

    @PutMapping("/ingredients/{id}")
    public Ingredient updateIngredient(@PathVariable UUID id, @RequestBody Ingredient ingredient) {
        Ingredient existing = ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
        existing.setName(ingredient.getName());
        existing.setQuantity(ingredient.getQuantity());
        existing.setUnit(ingredient.getUnit());
        existing.setThreshold(ingredient.getThreshold());
        return ingredientRepository.save(existing);
    }

    @PostMapping("/menu-link")
    public MenuIngredient linkMenuItem(@RequestBody Map<String, Object> req) {
        UUID menuId = UUID.fromString(req.get("menuItemId").toString());
        UUID ingId = UUID.fromString(req.get("ingredientId").toString());
        double qty = Double.parseDouble(req.get("quantityRequired").toString());

        MenuItem menuItem = menuItemRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu item not found"));
        Ingredient ingredient = ingredientRepository.findById(ingId)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        MenuIngredient mi = MenuIngredient.builder()
                .menuItem(menuItem)
                .ingredient(ingredient)
                .quantityRequired(qty)
                .build();

        return menuIngredientRepository.save(mi);
    }

}

