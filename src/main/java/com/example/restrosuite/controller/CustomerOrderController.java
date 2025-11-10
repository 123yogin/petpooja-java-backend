package com.example.restrosuite.controller;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@CrossOrigin(origins = "*") // Allow public access
public class CustomerOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OutletRepository outletRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Get menu items for customer ordering (public endpoint)
     */
    @GetMapping("/menu")
    public ResponseEntity<List<MenuItem>> getMenu() {
        try {
            List<MenuItem> menuItems = menuItemRepository.findByAvailableTrue();
            return ResponseEntity.ok(menuItems);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get table information (public endpoint)
     */
    @GetMapping("/table/{tableId}")
    public ResponseEntity<Map<String, Object>> getTableInfo(@PathVariable UUID tableId) {
        try {
            TableEntity table = tableRepository.findById(tableId)
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Check for existing active order
            java.util.Optional<Order> existingOrder = orderRepository.findLatestActiveOrderByTableId(tableId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", table.getId());
            response.put("tableNumber", table.getTableNumber());
            response.put("location", table.getLocation());
            response.put("capacity", table.getCapacity());
            
            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("orderId", order.getId());
                orderInfo.put("status", order.getStatus());
                orderInfo.put("totalAmount", order.getTotalAmount());
                orderInfo.put("createdAt", order.getCreatedAt());
                orderInfo.put("items", order.getItems());
                response.put("activeOrder", orderInfo);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Table not found");
            return ResponseEntity.status(404).body(error);
        }
    }

    /**
     * Create or update order from customer (public endpoint - no auth required)
     * If an active order exists for the table, items will be added to it.
     * Otherwise, a new order will be created.
     */
    @PostMapping("/order")
    public ResponseEntity<Map<String, Object>> createOrUpdateCustomerOrder(@RequestBody Map<String, Object> payload) {
        try {
            UUID tableId = UUID.fromString(payload.get("tableId").toString());
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) payload.get("items");

            if (itemsData == null || itemsData.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Order must contain at least one item");
                return ResponseEntity.badRequest().body(error);
            }

            TableEntity table = tableRepository.findById(tableId)
                    .orElseThrow(() -> new RuntimeException("Table not found"));

            // Check for existing active order
            java.util.Optional<Order> existingOrderOpt = orderRepository.findLatestActiveOrderByTableId(tableId);
            Order order;
            boolean isNewOrder = false;

            if (existingOrderOpt.isPresent()) {
                // Add items to existing order
                order = existingOrderOpt.get();
            } else {
                // Create new order
                Outlet outlet = table.getOutlet();
                order = Order.builder()
                        .table(table)
                        .outlet(outlet)
                        .status("CREATED")
                        .createdAt(LocalDateTime.now())
                        .totalAmount(0)
                        .items(new ArrayList<>())
                        .build();
                orderRepository.save(order);
                isNewOrder = true;
                
                // Mark table as occupied
                table.setOccupied(true);
                tableRepository.save(table);
            }

            // Process new items
            double additionalTotal = 0;
            List<OrderItem> newOrderItems = new ArrayList<>();

            for (Map<String, Object> i : itemsData) {
                UUID menuId = UUID.fromString(i.get("menuItemId").toString());
                int qty = Integer.parseInt(i.get("quantity").toString());

                MenuItem menuItem = menuItemRepository.findById(menuId)
                        .orElseThrow(() -> new RuntimeException("Menu item not found: " + menuId));

                if (!menuItem.isAvailable()) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Menu item " + menuItem.getName() + " is not available");
                    return ResponseEntity.badRequest().body(error);
                }

                // Check if item already exists in order
                boolean itemExists = false;
                for (OrderItem existingItem : order.getItems()) {
                    if (existingItem.getMenuItem().getId().equals(menuId)) {
                        // Update quantity and price for existing item
                        existingItem.setQuantity(existingItem.getQuantity() + qty);
                        existingItem.setPrice(existingItem.getPrice() + (menuItem.getPrice() * qty));
                        additionalTotal += menuItem.getPrice() * qty;
                        itemExists = true;
                        break;
                    }
                }

                if (!itemExists) {
                    // Add new item
                    double price = menuItem.getPrice() * qty;
                    additionalTotal += price;

                    OrderItem newItem = OrderItem.builder()
                            .menuItem(menuItem)
                            .quantity(qty)
                            .price(price)
                            .order(order)
                            .build();
                    newOrderItems.add(newItem);
                }
            }

            // Add new items to order
            order.getItems().addAll(newOrderItems);
            
            // Update total amount
            order.setTotalAmount(order.getTotalAmount() + additionalTotal);
            
            Order savedOrder = orderRepository.save(order);
            
            // Broadcast order update to all connected clients
            messagingTemplate.convertAndSend("/topic/orders", savedOrder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", savedOrder.getId());
            response.put("isNewOrder", isNewOrder);
            response.put("message", isNewOrder ? "Order placed successfully!" : "Items added to existing order!");
            response.put("totalAmount", savedOrder.getTotalAmount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create/update order: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get order status (public endpoint)
     */
    @GetMapping("/order/{orderId}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("status", order.getStatus());
            response.put("totalAmount", order.getTotalAmount());
            response.put("createdAt", order.getCreatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Order not found");
            return ResponseEntity.status(404).body(error);
        }
    }
}

