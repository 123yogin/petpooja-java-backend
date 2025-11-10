package com.example.restrosuite.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.restrosuite.entity.Bill;
import com.example.restrosuite.entity.Customer;
import com.example.restrosuite.entity.Ingredient;
import com.example.restrosuite.entity.MenuIngredient;
import com.example.restrosuite.entity.MenuItem;
import com.example.restrosuite.entity.Order;
import com.example.restrosuite.entity.OrderItem;
import com.example.restrosuite.entity.TableEntity;
import com.example.restrosuite.repository.BillRepository;
import com.example.restrosuite.repository.CustomerRepository;
import com.example.restrosuite.repository.IngredientRepository;
import com.example.restrosuite.repository.TableRepository;
import com.example.restrosuite.repository.MenuIngredientRepository;
import com.example.restrosuite.repository.OrderRepository;
import com.example.restrosuite.service.InvoiceService;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private MenuIngredientRepository menuIngredientRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TableRepository tableRepository;

    // Default restaurant GSTIN and state (can be configured)
    private static final String RESTAURANT_GSTIN = "29ABCDE1234F1Z5"; // Example GSTIN
    private static final String RESTAURANT_STATE = "29"; // Example state code (Karnataka)

    @PostMapping("/generate/{orderId}")
    public Bill generateBill(@PathVariable UUID orderId, @RequestBody(required = false) Map<String, Object> request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("Order not completed yet");
        }

        // Check if bill already exists for this order
        Optional<Bill> existingBill = billRepository.findByOrderId(orderId);
        if (existingBill.isPresent()) {
            return existingBill.get(); // Return existing bill instead of creating duplicate
        }

        // Get customer if provided
        Customer customer = null;
        String customerGstin = null;
        String placeOfSupply = RESTAURANT_STATE; // Default to restaurant state
        boolean isInterState = false;

        if (request != null && request.containsKey("customerId")) {
            UUID customerId = UUID.fromString(request.get("customerId").toString());
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            customerGstin = customer.getGstin();
            if (customer.getState() != null && !customer.getState().equals(RESTAURANT_STATE)) {
                isInterState = true;
                placeOfSupply = customer.getState();
            }
        }

        // Calculate GST for each item
        double totalTaxableAmount = 0.0;
        double totalCgst = 0.0;
        double totalSgst = 0.0;
        double totalIgst = 0.0;

        for (OrderItem item : order.getItems()) {
            MenuItem menuItem = item.getMenuItem();
            double itemAmount = item.getPrice();
            double itemTaxRate = (menuItem.getTaxRate() != null && menuItem.getTaxRate() > 0) 
                ? menuItem.getTaxRate() : 5.0; // Default 5% if not set
            double itemTax = itemAmount * (itemTaxRate / 100.0);
            
            totalTaxableAmount += itemAmount;
            
            if (isInterState) {
                // Inter-state: IGST
                totalIgst += itemTax;
            } else {
                // Intra-state: CGST + SGST (split equally)
                totalCgst += itemTax / 2.0;
                totalSgst += itemTax / 2.0;
            }
        }

        double totalTax = totalCgst + totalSgst + totalIgst;
        double discountAmount = (request != null && request.containsKey("discountAmount")) 
            ? Double.parseDouble(request.get("discountAmount").toString()) 
            : 0.0;
        double grandTotal = totalTaxableAmount + totalTax - discountAmount;

        Bill bill = Bill.builder()
                .order(order)
                .customer(customer)
                .totalAmount(totalTaxableAmount)
                .tax(totalTax)
                .discountAmount(discountAmount)
                .grandTotal(grandTotal)
                .generatedAt(LocalDateTime.now())
                .companyGstin(RESTAURANT_GSTIN)
                .customerGstin(customerGstin)
                .cgst(totalCgst)
                .sgst(totalSgst)
                .igst(totalIgst)
                .placeOfSupply(placeOfSupply)
                .isInterState(isInterState)
                .paymentStatus("PENDING")
                .paidAmount(0.0)
                .pendingAmount(grandTotal)
                .build();

        Bill savedBill = billRepository.save(bill);

        // Mark table as vacant after bill is generated (for single order bills)
        // Note: For combined bills, this is handled in generateCombinedBillForTable
        if (order.getTable() != null) {
            // Check if there are any other unbilled completed orders for this table
            List<Order> otherCompletedOrders = orderRepository.findCompletedOrdersByTableId(order.getTable().getId());
            boolean hasOtherUnbilledOrders = false;
            for (Order otherOrder : otherCompletedOrders) {
                if (!otherOrder.getId().equals(order.getId())) {
                    Optional<Bill> otherBill = billRepository.findByOrderId(otherOrder.getId());
                    if (otherBill.isEmpty()) {
                        hasOtherUnbilledOrders = true;
                        break;
                    }
                }
            }
            
            // Only mark table as vacant if all orders for this table are billed
            if (!hasOtherUnbilledOrders) {
                TableEntity table = order.getTable();
                table.setOccupied(false);
                tableRepository.save(table);
            }
        }

        // Auto-deduct inventory after billing
        for (OrderItem item : order.getItems()) {
            List<MenuIngredient> links = menuIngredientRepository.findAll()
                    .stream()
                    .filter(link -> link.getMenuItem().getId().equals(item.getMenuItem().getId()))
                    .toList();

            for (MenuIngredient link : links) {
                Ingredient ing = ingredientRepository.findById(link.getIngredient().getId())
                        .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                double used = link.getQuantityRequired() * item.getQuantity();
                ing.setQuantity(ing.getQuantity() - used);
                ingredientRepository.save(ing);
            }
        }

        return savedBill;
    }

    @GetMapping("/{id}")
    public Bill getBill(@PathVariable UUID id) {
        return billRepository.findById(id).orElseThrow();
    }

    @GetMapping("/order/{orderId}")
    public Bill getBillByOrderId(@PathVariable UUID orderId) {
        return billRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Bill not found for this order"));
    }

    @GetMapping
    public List<Bill> getAllBills() {
        return billRepository.findAllByOrderByGeneratedAtDesc();
    }

    @GetMapping("/{id}/invoice")
    public String getInvoiceText(@PathVariable UUID id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        return invoiceService.generateTextInvoice(bill);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<Map<String, Object>> getBillItems(@PathVariable UUID id) {
        try {
            Bill bill = billRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Bill not found"));
            
            Order primaryOrder = bill.getOrder();
            List<Order> allOrders = new ArrayList<>();
            List<Map<String, Object>> allItems = new ArrayList<>();
            
            if (primaryOrder != null && primaryOrder.getTable() != null) {
                if ("COMBINED_BILL".equals(bill.getPaymentStatus())) {
                    List<Order> tableOrders = orderRepository.findCompletedOrdersByTableId(primaryOrder.getTable().getId());
                    final java.time.LocalDateTime billGeneratedAt = bill.getGeneratedAt();
                    allOrders = tableOrders.stream()
                        .filter(order -> {
                            Optional<Bill> orderBill = billRepository.findByOrderId(order.getId());
                            if (orderBill.isPresent() && "COMBINED".equals(orderBill.get().getPaymentStatus())) {
                                // Check if this order's bill was generated around the same time as the combined bill
                                long timeDiff = Math.abs(java.time.Duration.between(
                                    orderBill.get().getGeneratedAt(), 
                                    billGeneratedAt
                                ).toMinutes());
                                return timeDiff <= 1; // Orders billed within 1 minute are part of this combined bill
                            }
                            return false;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    if (!allOrders.contains(primaryOrder)) {
                        allOrders.add(0, primaryOrder);
                    }
                } else {
                    allOrders.add(primaryOrder);
                }
                
                // Collect all items from all orders
                java.util.Map<UUID, Map<String, Object>> itemMap = new java.util.HashMap<>();
                for (Order order : allOrders) {
                    if (order.getItems() != null) {
                        for (OrderItem item : order.getItems()) {
                            UUID menuItemId = item.getMenuItem().getId();
                            if (itemMap.containsKey(menuItemId)) {
                                Map<String, Object> existing = itemMap.get(menuItemId);
                                existing.put("quantity", (Integer)existing.get("quantity") + item.getQuantity());
                                existing.put("price", (Double)existing.get("price") + item.getPrice());
                            } else {
                                Map<String, Object> itemData = new HashMap<>();
                                itemData.put("menuItemId", menuItemId);
                                itemData.put("name", item.getMenuItem().getName());
                                itemData.put("hsnCode", item.getMenuItem().getHsnCode());
                                itemData.put("quantity", item.getQuantity());
                                itemData.put("unitPrice", item.getMenuItem().getPrice());
                                itemData.put("price", item.getPrice());
                                itemMap.put(menuItemId, itemData);
                            }
                        }
                    }
                }
                allItems = new ArrayList<>(itemMap.values());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("items", allItems);
            response.put("orderCount", allOrders.size());
            response.put("isCombinedBill", "COMBINED_BILL".equals(bill.getPaymentStatus()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get bill items: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/download/{billId}")
    public ResponseEntity<?> downloadInvoice(@PathVariable UUID billId) {
        try {
            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new RuntimeException("Bill not found"));
            
            byte[] pdf = invoiceService.generateInvoicePdf(bill);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("invoice-" + bill.getId() + ".pdf").build());

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating PDF: " + e.getMessage());
        }
    }

    /**
     * Generate a combined bill for all completed orders of a table
     * This combines all orders into one bill
     */
    @PostMapping("/generate/table/{tableId}")
    public Bill generateCombinedBillForTable(@PathVariable UUID tableId, @RequestBody(required = false) Map<String, Object> request) {
        // Get the most recent bill for this table to determine the cutoff time
        // Only include orders created after the last bill was generated
        List<Bill> previousBills = billRepository.findBillsByTableId(tableId);
        final java.time.LocalDateTime lastBillTime;
        
        if (!previousBills.isEmpty()) {
            // Find the most recent bill (should be first in DESC order)
            Bill lastBill = previousBills.get(0);
            lastBillTime = lastBill.getGeneratedAt();
        } else {
            lastBillTime = null;
        }
        
        // Get all completed orders for this table that don't have bills yet
        List<Order> completedOrders = orderRepository.findCompletedOrdersByTableId(tableId);
        
        if (completedOrders.isEmpty()) {
            throw new RuntimeException("No completed orders found for this table");
        }

        // Filter out orders that already have bills AND orders created before the last bill
        final java.time.LocalDateTime cutoffTime = lastBillTime;
        List<Order> ordersWithoutBills = completedOrders.stream()
                .filter(order -> {
                    // Check if order already has a bill
                    Optional<Bill> existingBill = billRepository.findByOrderId(order.getId());
                    if (existingBill.isPresent()) {
                        return false; // Skip orders that already have bills
                    }
                    
                    // If there was a previous bill, only include orders created after it
                    if (cutoffTime != null && order.getCreatedAt() != null) {
                        return order.getCreatedAt().isAfter(cutoffTime);
                    }
                    
                    // If no previous bill, include all unbilled orders
                    return true;
                })
                .toList();

        if (ordersWithoutBills.isEmpty()) {
            throw new RuntimeException("No new orders found for this table since the last bill");
        }

        // Get customer if provided
        Customer customer = null;
        String customerGstin = null;
        String placeOfSupply = RESTAURANT_STATE;
        boolean isInterState = false;

        if (request != null && request.containsKey("customerId")) {
            UUID customerId = UUID.fromString(request.get("customerId").toString());
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            customerGstin = customer.getGstin();
            if (customer.getState() != null && !customer.getState().equals(RESTAURANT_STATE)) {
                isInterState = true;
                placeOfSupply = customer.getState();
            }
        }

        // Combine all order items and calculate totals
        double totalTaxableAmount = 0.0;
        double totalCgst = 0.0;
        double totalSgst = 0.0;
        double totalIgst = 0.0;
        List<OrderItem> allOrderItems = new java.util.ArrayList<>();

        for (Order order : ordersWithoutBills) {
            for (OrderItem item : order.getItems()) {
                MenuItem menuItem = item.getMenuItem();
                double itemAmount = item.getPrice();
                double itemTaxRate = (menuItem.getTaxRate() != null && menuItem.getTaxRate() > 0) 
                    ? menuItem.getTaxRate() : 5.0;
                double itemTax = itemAmount * (itemTaxRate / 100.0);
                
                totalTaxableAmount += itemAmount;
                
                if (isInterState) {
                    totalIgst += itemTax;
                } else {
                    totalCgst += itemTax / 2.0;
                    totalSgst += itemTax / 2.0;
                }
                
                allOrderItems.add(item);
            }
        }

        double totalTax = totalCgst + totalSgst + totalIgst;
        double discountAmount = (request != null && request.containsKey("discountAmount")) 
            ? Double.parseDouble(request.get("discountAmount").toString()) 
            : 0.0;
        double grandTotal = totalTaxableAmount + totalTax - discountAmount;

        // Use the first order as the primary order for the bill
        // In a real scenario, you might want to create a separate TableBill entity
        Order primaryOrder = ordersWithoutBills.get(0);

        Bill bill = Bill.builder()
                .order(primaryOrder) // Link to first order (for compatibility)
                .customer(customer)
                .totalAmount(totalTaxableAmount)
                .tax(totalTax)
                .discountAmount(discountAmount)
                .grandTotal(grandTotal)
                .generatedAt(LocalDateTime.now())
                .companyGstin(RESTAURANT_GSTIN)
                .customerGstin(customerGstin)
                .cgst(totalCgst)
                .sgst(totalSgst)
                .igst(totalIgst)
                .placeOfSupply(placeOfSupply)
                .isInterState(isInterState)
                .paymentStatus("COMBINED_BILL") // Special status to identify combined bills
                .paidAmount(0.0)
                .pendingAmount(grandTotal)
                .build();

        Bill savedBill = billRepository.save(bill);

        // Mark all orders as billed (you might want to add a field for this)
        // For now, we'll create individual bills for each order to maintain data integrity
        // But the combined bill represents the total
        
        // Create individual bills for each order (for tracking)
        for (Order order : ordersWithoutBills) {
            if (billRepository.findByOrderId(order.getId()).isEmpty()) {
                // Create a reference bill for each order
                Bill orderBill = Bill.builder()
                        .order(order)
                        .customer(customer)
                        .totalAmount(order.getTotalAmount())
                        .tax(0.0) // Tax already calculated in combined bill
                        .discountAmount(0.0)
                        .grandTotal(order.getTotalAmount())
                        .generatedAt(LocalDateTime.now())
                        .companyGstin(RESTAURANT_GSTIN)
                        .customerGstin(customerGstin)
                        .cgst(0.0)
                        .sgst(0.0)
                        .igst(0.0)
                        .placeOfSupply(placeOfSupply)
                        .isInterState(isInterState)
                        .paymentStatus("COMBINED") // Special status for combined bills
                        .paidAmount(0.0)
                        .pendingAmount(0.0)
                        .build();
                billRepository.save(orderBill);
            }
        }

        // Auto-deduct inventory after billing
        for (OrderItem item : allOrderItems) {
            List<MenuIngredient> links = menuIngredientRepository.findAll()
                    .stream()
                    .filter(link -> link.getMenuItem().getId().equals(item.getMenuItem().getId()))
                    .toList();

            for (MenuIngredient link : links) {
                Ingredient ing = ingredientRepository.findById(link.getIngredient().getId())
                        .orElseThrow(() -> new RuntimeException("Ingredient not found"));
                double used = link.getQuantityRequired() * item.getQuantity();
                ing.setQuantity(ing.getQuantity() - used);
                ingredientRepository.save(ing);
            }
        }

        // Mark table as vacant after bill is generated
        if (primaryOrder.getTable() != null) {
            TableEntity table = primaryOrder.getTable();
            table.setOccupied(false);
            tableRepository.save(table);
        }

        return savedBill;
    }

}

