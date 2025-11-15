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
    private BillRepository billRepository;

    @Autowired
    private MenuIngredientRepository menuIngredientRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private MenuModifierRepository menuModifierRepository;

    @Autowired
    private com.example.restrosuite.service.InvoiceService invoiceService;

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
            
            // Check for existing active order (CREATED or IN_PROGRESS only)
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
            } else {
                // Check if there are completed orders that don't have bills yet
                // Return the most recent completed order so customer can generate bill
                List<Order> completedOrders = orderRepository.findCompletedOrdersByTableId(tableId);
                if (!completedOrders.isEmpty()) {
                    // Get the most recent completed order (last in the list since it's ordered by createdAt ASC)
                    Order latestCompletedOrder = completedOrders.get(completedOrders.size() - 1);
                    
                    // Check if it has a bill
                    java.util.Optional<Bill> bill = billRepository.findByOrderId(latestCompletedOrder.getId());
                    
                    // Return the completed order so customer can generate bill
                    Map<String, Object> orderInfo = new HashMap<>();
                    orderInfo.put("orderId", latestCompletedOrder.getId());
                    orderInfo.put("status", latestCompletedOrder.getStatus());
                    orderInfo.put("totalAmount", latestCompletedOrder.getTotalAmount());
                    orderInfo.put("createdAt", latestCompletedOrder.getCreatedAt());
                    orderInfo.put("items", latestCompletedOrder.getItems());
                    orderInfo.put("hasBill", bill.isPresent());
                    response.put("completedOrder", orderInfo);
                    
                    // Also check if there are other unbilled orders
                    boolean hasUnbilledOrders = false;
                    int unbilledCount = 0;
                    for (Order order : completedOrders) {
                        java.util.Optional<Bill> orderBill = billRepository.findByOrderId(order.getId());
                        if (orderBill.isEmpty()) {
                            hasUnbilledOrders = true;
                            unbilledCount++;
                        }
                    }
                    
                    if (hasUnbilledOrders) {
                        response.put("hasCompletedOrders", true);
                        response.put("completedOrdersCount", unbilledCount);
                    }
                }
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
                // Before creating a new order, check if there are unbilled completed orders
                // Session should remain active until bills are generated
                List<Order> completedOrders = orderRepository.findCompletedOrdersByTableId(tableId);
                boolean hasUnbilledOrders = false;
                for (Order completedOrder : completedOrders) {
                    java.util.Optional<Bill> bill = billRepository.findByOrderId(completedOrder.getId());
                    if (bill.isEmpty()) {
                        hasUnbilledOrders = true;
                        break;
                    }
                }
                
                if (hasUnbilledOrders) {
                    // Cannot create new order - previous session's bills not generated yet
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Cannot create new order. Please wait for the previous orders to be billed first.");
                    error.put("hasUnbilledOrders", true);
                    return ResponseEntity.status(400).body(error);
                }
                
                // Ensure table is marked as occupied (in case it was manually changed)
                if (!table.isOccupied()) {
                    table.setOccupied(true);
                    tableRepository.save(table);
                }
                
                // Create new order - only allowed if all previous orders are billed
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
                // Note: For simplicity, we treat items with modifiers as different items
                // In production, you might want to match items with same modifiers
                boolean itemExists = false;
                boolean hasModifiers = i.containsKey("modifiers") && i.get("modifiers") != null;
                
                if (!hasModifiers) {
                    // Only match items without modifiers
                    for (OrderItem existingItem : order.getItems()) {
                        if (existingItem.getMenuItem().getId().equals(menuId) && 
                            (existingItem.getModifiers() == null || existingItem.getModifiers().isEmpty())) {
                            // Update quantity and price for existing item
                            existingItem.setQuantity(existingItem.getQuantity() + qty);
                            // Note: price is per unit, so we just update quantity
                            additionalTotal += menuItem.getPrice() * qty;
                            itemExists = true;
                            break;
                        }
                    }
                }

                if (!itemExists) {
                    // Add new item
                    double basePrice = menuItem.getPrice();
                    
                    // Process modifiers if provided
                    List<OrderItemModifier> modifiers = new ArrayList<>();
                    double modifierTotal = 0.0;
                    if (i.containsKey("modifiers") && i.get("modifiers") != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> modifierDataList = (List<Map<String, Object>>) i.get("modifiers");
                        
                        for (Map<String, Object> modifierData : modifierDataList) {
                            UUID modifierId = UUID.fromString(modifierData.get("modifierId").toString());
                            MenuModifier menuModifier = menuModifierRepository.findById(modifierId)
                                    .orElseThrow(() -> new RuntimeException("Menu modifier not found: " + modifierId));
                            
                            if (!menuModifier.getIsActive()) {
                                Map<String, Object> error = new HashMap<>();
                                error.put("error", "Modifier " + menuModifier.getName() + " is not available");
                                return ResponseEntity.badRequest().body(error);
                            }

                            OrderItemModifier orderItemModifier = OrderItemModifier.builder()
                                    .menuModifier(menuModifier)
                                    .price(menuModifier.getPrice())
                                    .build();
                            modifiers.add(orderItemModifier);
                            modifierTotal += menuModifier.getPrice();
                        }
                    }
                    
                    double itemTotal = (basePrice + modifierTotal) * qty;
                    additionalTotal += itemTotal;

                    OrderItem newItem = OrderItem.builder()
                            .menuItem(menuItem)
                            .quantity(qty)
                            .price(basePrice) // Store base price per unit
                            .modifiers(modifiers)
                            .order(order)
                            .build();
                    
                    // Set back reference for modifiers
                    for (OrderItemModifier modifier : modifiers) {
                        modifier.setOrderItem(newItem);
                    }
                    
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

    /**
     * Generate bill for an order (customer endpoint - public)
     * Allows customers to generate bills for their orders at any time
     */
    @PostMapping("/order/{orderId}/generate-bill")
    public ResponseEntity<Map<String, Object>> generateCustomerBill(
            @PathVariable UUID orderId,
            @RequestParam UUID tableId) {
        try {
            // Verify order exists and belongs to the table
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (order.getTable() == null || !order.getTable().getId().equals(tableId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Order does not belong to this table");
                return ResponseEntity.status(403).body(error);
            }
            
            // Only allow bill generation for completed orders
            if (!order.getStatus().equals("COMPLETED")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Order must be completed before generating bill");
                return ResponseEntity.status(400).body(error);
            }
            
            // Check if bill already exists
            java.util.Optional<Bill> existingBill = billRepository.findByOrderId(orderId);
            Bill bill;
            
            if (existingBill.isPresent()) {
                bill = existingBill.get();
            } else {
                // Generate new bill (walk-in customer, no customer GSTIN)
                bill = generateBillForOrder(order);
            }
            
            // Get bill items
            List<Map<String, Object>> billItems = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("menuItemId", item.getMenuItem().getId());
                itemData.put("name", item.getMenuItem().getName());
                itemData.put("hsnCode", item.getMenuItem().getHsnCode());
                itemData.put("quantity", item.getQuantity());
                itemData.put("unitPrice", item.getMenuItem().getPrice());
                itemData.put("price", item.getPrice());
                billItems.add(itemData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("billId", bill.getId());
            response.put("orderId", order.getId());
            response.put("totalAmount", bill.getTotalAmount());
            response.put("tax", bill.getTax());
            response.put("cgst", bill.getCgst());
            response.put("sgst", bill.getSgst());
            response.put("igst", bill.getIgst());
            response.put("discountAmount", bill.getDiscountAmount());
            response.put("grandTotal", bill.getGrandTotal());
            response.put("generatedAt", bill.getGeneratedAt());
            response.put("items", billItems);
            response.put("isInterState", bill.getIsInterState());
            response.put("placeOfSupply", bill.getPlaceOfSupply());
            response.put("companyGstin", bill.getCompanyGstin());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate bill: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Helper method to generate bill for an order
     */
    private Bill generateBillForOrder(Order order) {
        final String RESTAURANT_GSTIN = "29ABCDE1234F1Z5";
        final String RESTAURANT_STATE = "29";
        
        // Calculate GST
        double totalTaxableAmount = 0.0;
        double totalCgst = 0.0;
        double totalSgst = 0.0;
        double totalIgst = 0.0;
        
        for (OrderItem item : order.getItems()) {
            MenuItem menuItem = item.getMenuItem();
            double itemAmount = item.getPrice();
            double itemTaxRate = (menuItem.getTaxRate() != null && menuItem.getTaxRate() > 0) 
                ? menuItem.getTaxRate() : 5.0;
            double itemTax = itemAmount * (itemTaxRate / 100.0);
            
            totalTaxableAmount += itemAmount;
            // Intra-state: CGST + SGST (split equally)
            totalCgst += itemTax / 2.0;
            totalSgst += itemTax / 2.0;
        }
        
        double totalTax = totalCgst + totalSgst + totalIgst;
        double grandTotal = totalTaxableAmount + totalTax;
        
        Bill bill = Bill.builder()
                .order(order)
                .customer(null)
                .totalAmount(totalTaxableAmount)
                .tax(totalTax)
                .discountAmount(0.0)
                .grandTotal(grandTotal)
                .generatedAt(LocalDateTime.now())
                .companyGstin(RESTAURANT_GSTIN)
                .customerGstin(null)
                .cgst(totalCgst)
                .sgst(totalSgst)
                .igst(totalIgst)
                .placeOfSupply(RESTAURANT_STATE)
                .isInterState(false)
                .paymentStatus("PENDING")
                .paidAmount(0.0)
                .pendingAmount(grandTotal)
                .build();
        
        Bill savedBill = billRepository.save(bill);
        
        // Auto-deduct inventory
        for (OrderItem item : order.getItems()) {
            List<com.example.restrosuite.entity.MenuIngredient> links = menuIngredientRepository.findAll()
                    .stream()
                    .filter(link -> link.getMenuItem().getId().equals(item.getMenuItem().getId()))
                    .toList();

            for (com.example.restrosuite.entity.MenuIngredient link : links) {
                com.example.restrosuite.entity.Ingredient ing = ingredientRepository.findById(link.getIngredient().getId())
                        .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                double used = link.getQuantityRequired() * item.getQuantity();
                ing.setQuantity(ing.getQuantity() - used);
                ingredientRepository.save(ing);
            }
        }
        
        // Mark table as vacant if all orders for this table are billed
        if (order.getTable() != null) {
            List<Order> otherCompletedOrders = orderRepository.findCompletedOrdersByTableId(order.getTable().getId());
            boolean hasOtherUnbilledOrders = false;
            for (Order otherOrder : otherCompletedOrders) {
                if (!otherOrder.getId().equals(order.getId())) {
                    java.util.Optional<Bill> otherBill = billRepository.findByOrderId(otherOrder.getId());
                    if (otherBill.isEmpty()) {
                        hasOtherUnbilledOrders = true;
                        break;
                    }
                }
            }
            
            if (!hasOtherUnbilledOrders) {
                TableEntity table = order.getTable();
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }
        
        return savedBill;
    }

    /**
     * Download bill PDF for a customer (public endpoint)
     */
    @GetMapping("/bill/{billId}/download")
    public ResponseEntity<?> downloadCustomerBill(
            @PathVariable UUID billId,
            @RequestParam UUID tableId) {
        try {
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new RuntimeException("Bill not found"));
            
            // Verify bill belongs to the table
            if (bill.getOrder() == null || bill.getOrder().getTable() == null 
                    || !bill.getOrder().getTable().getId().equals(tableId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bill does not belong to this table");
                return ResponseEntity.status(403).body(error);
            }
            
            // Generate PDF
            byte[] pdf = invoiceService.generateInvoicePdf(bill);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                .filename("invoice-" + bill.getId() + ".pdf").build());
            
            return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to download bill: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

