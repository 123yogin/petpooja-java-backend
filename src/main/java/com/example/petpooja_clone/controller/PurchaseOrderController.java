package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Ingredient;
import com.example.petpooja_clone.entity.PurchaseOrder;
import com.example.petpooja_clone.entity.Supplier;
import com.example.petpooja_clone.repository.IngredientRepository;
import com.example.petpooja_clone.repository.PurchaseOrderRepository;
import com.example.petpooja_clone.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @GetMapping
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    @GetMapping("/{id}")
    public PurchaseOrder getPurchaseOrder(@PathVariable UUID id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
    }

    @PostMapping
    public PurchaseOrder createPurchaseOrder(@RequestBody Map<String, Object> payload) {
        UUID supplierId = UUID.fromString(payload.get("supplierId").toString());
        UUID ingredientId = UUID.fromString(payload.get("ingredientId").toString());
        double quantity = Double.parseDouble(payload.get("quantity").toString());
        double cost = Double.parseDouble(payload.get("cost").toString());

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .ingredient(ingredient)
                .quantity(quantity)
                .cost(cost)
                .date(LocalDateTime.now())
                .build();

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);

        // Auto-update ingredient quantity when purchase order is created
        ingredient.setQuantity(ingredient.getQuantity() + quantity);
        ingredientRepository.save(ingredient);

        return saved;
    }

    @PutMapping("/{id}")
    public PurchaseOrder updatePurchaseOrder(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        PurchaseOrder existing = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        // Revert old quantity if ingredient changed
        if (payload.containsKey("ingredientId")) {
            UUID newIngredientId = UUID.fromString(payload.get("ingredientId").toString());
            if (!existing.getIngredient().getId().equals(newIngredientId)) {
                // Revert old ingredient quantity
                Ingredient oldIngredient = existing.getIngredient();
                oldIngredient.setQuantity(oldIngredient.getQuantity() - existing.getQuantity());
                ingredientRepository.save(oldIngredient);

                // Update to new ingredient
                Ingredient newIngredient = ingredientRepository.findById(newIngredientId)
                        .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                existing.setIngredient(newIngredient);
            }
        }

        if (payload.containsKey("supplierId")) {
            UUID supplierId = UUID.fromString(payload.get("supplierId").toString());
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));
            existing.setSupplier(supplier);
        }

        if (payload.containsKey("quantity")) {
            double newQuantity = Double.parseDouble(payload.get("quantity").toString());
            double oldQuantity = existing.getQuantity();
            
            // Update ingredient quantity based on difference
            Ingredient ingredient = existing.getIngredient();
            ingredient.setQuantity(ingredient.getQuantity() - oldQuantity + newQuantity);
            ingredientRepository.save(ingredient);
            
            existing.setQuantity(newQuantity);
        }

        if (payload.containsKey("cost")) {
            existing.setCost(Double.parseDouble(payload.get("cost").toString()));
        }

        return purchaseOrderRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deletePurchaseOrder(@PathVariable UUID id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        // Revert ingredient quantity when purchase order is deleted
        Ingredient ingredient = purchaseOrder.getIngredient();
        ingredient.setQuantity(ingredient.getQuantity() - purchaseOrder.getQuantity());
        ingredientRepository.save(ingredient);

        purchaseOrderRepository.deleteById(id);
    }
}

