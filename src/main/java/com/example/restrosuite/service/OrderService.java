package com.example.restrosuite.service;

import com.example.restrosuite.entity.*;
import com.example.restrosuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OutletRepository outletRepository;

    @Autowired
    private MenuModifierRepository menuModifierRepository;

    @Autowired
    private OrderItemModifierRepository orderItemModifierRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Order createOrder(Map<String, Object> payload) {
        UUID tableId = UUID.fromString(payload.get("tableId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) payload.get("items");

        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        Outlet outlet = null;
        if (payload.containsKey("outletId") && payload.get("outletId") != null) {
            UUID outletId = UUID.fromString(payload.get("outletId").toString());
            outlet = outletRepository.findById(outletId)
                    .orElseThrow(() -> new RuntimeException("Outlet not found"));
        }

        double total = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map<String, Object> itemData : itemsData) {
            UUID menuId = UUID.fromString(itemData.get("menuItemId").toString());
            int qty = Integer.parseInt(itemData.get("quantity").toString());

            MenuItem menuItem = menuItemRepository.findById(menuId)
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + menuId));

            if (!menuItem.isAvailable()) {
                throw new RuntimeException("Menu item " + menuItem.getName() + " is not available");
            }

            double basePrice = menuItem.getPrice(); // Price per unit

            // Process modifiers if provided
            List<OrderItemModifier> modifiers = new ArrayList<>();
            double modifierTotal = 0.0;
            if (itemData.containsKey("modifiers") && itemData.get("modifiers") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> modifierDataList = (List<Map<String, Object>>) itemData.get("modifiers");
                
                for (Map<String, Object> modifierData : modifierDataList) {
                    UUID modifierId = UUID.fromString(modifierData.get("modifierId").toString());
                    MenuModifier menuModifier = menuModifierRepository.findById(modifierId)
                            .orElseThrow(() -> new RuntimeException("Menu modifier not found: " + modifierId));
                    
                    if (!menuModifier.getIsActive()) {
                        throw new RuntimeException("Modifier " + menuModifier.getName() + " is not available");
                    }

                    OrderItemModifier orderItemModifier = OrderItemModifier.builder()
                            .menuModifier(menuModifier)
                            .price(menuModifier.getPrice())
                            .build();
                    modifiers.add(orderItemModifier);
                    modifierTotal += menuModifier.getPrice(); // Add modifier price per unit
                }
            }

            // Calculate total: (basePrice + modifierTotal) * quantity
            double itemTotal = (basePrice + modifierTotal) * qty;
            total += itemTotal;

            OrderItem orderItem = OrderItem.builder()
                    .menuItem(menuItem)
                    .quantity(qty)
                    .price(basePrice) // Store base price per unit
                    .modifiers(modifiers)
                    .build();

            // Set back reference for modifiers
            for (OrderItemModifier modifier : modifiers) {
                modifier.setOrderItem(orderItem);
            }

            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .table(table)
                .outlet(outlet)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .totalAmount(total)
                .items(orderItems)
                .build();

        // Set order reference for all order items
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
        }

        Order savedOrder = orderRepository.save(order);
        
        // Broadcast new order to all connected clients
        messagingTemplate.convertAndSend("/topic/orders", savedOrder);
        
        return savedOrder;
    }

    public Order updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        // Broadcast order status update to all connected clients
        messagingTemplate.convertAndSend("/topic/orders", updatedOrder);
        
        return updatedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}

