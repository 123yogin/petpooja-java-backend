package com.example.restrosuite.controller;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

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

    @PostMapping("/create")
    @SuppressWarnings("unchecked")
    public Order createOrder(@RequestBody Map<String, Object> payload) {
        UUID tableId = UUID.fromString(payload.get("tableId").toString());
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) payload.get("items");

        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        // Get outlet if provided
        Outlet outlet = null;
        if (payload.containsKey("outletId") && payload.get("outletId") != null) {
            UUID outletId = UUID.fromString(payload.get("outletId").toString());
            outlet = outletRepository.findById(outletId)
                    .orElseThrow(() -> new RuntimeException("Outlet not found"));
        }

        double total = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> i : itemsData) {
            UUID menuId = UUID.fromString(i.get("menuItemId").toString());
            int qty = (int) i.get("quantity");

            MenuItem menuItem = menuItemRepository.findById(menuId)
                    .orElseThrow(() -> new RuntimeException("Menu item not found"));

            double price = menuItem.getPrice() * qty;
            total += price;

            orderItems.add(OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(qty)
                    .price(price)
                    .build());
        }

        Order order = Order.builder()
                .table(table)
                .outlet(outlet)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .totalAmount(total)
                .items(new ArrayList<>())
                .build();

        orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
        }

        order.getItems().addAll(orderItems);
        Order savedOrder = orderRepository.save(order);
        
        // Broadcast new order to all connected clients
        messagingTemplate.convertAndSend("/topic/orders", savedOrder);
        
        return savedOrder;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable UUID id, @RequestParam String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        // Broadcast order status update to all connected clients
        messagingTemplate.convertAndSend("/topic/orders", updatedOrder);
        
        return updatedOrder;
    }

}

