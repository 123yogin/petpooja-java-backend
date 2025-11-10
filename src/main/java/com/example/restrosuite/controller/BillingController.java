package com.example.restrosuite.controller;

import java.time.LocalDateTime;
import java.util.List;
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
import com.example.restrosuite.repository.BillRepository;
import com.example.restrosuite.repository.CustomerRepository;
import com.example.restrosuite.repository.IngredientRepository;
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

}

