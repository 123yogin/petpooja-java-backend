package com.example.restrosuite.service;

import com.example.restrosuite.entity.Bill;
import com.example.restrosuite.entity.Order;
import com.example.restrosuite.entity.OrderItem;
import com.example.restrosuite.repository.BillRepository;
import com.example.restrosuite.repository.OrderRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private BillRepository billRepository;

    public String generateTextInvoice(Bill bill) {
        Order primaryOrder = bill.getOrder();
        StringBuilder invoice = new StringBuilder();
        
        // Get all orders for combined bills
        List<Order> allOrders = new ArrayList<>();
        List<OrderItem> allItems = new ArrayList<>();
        
        if (primaryOrder != null && primaryOrder.getTable() != null) {
            // Check if this is a combined bill
            if ("COMBINED_BILL".equals(bill.getPaymentStatus())) {
                // Get all completed orders for this table that have COMBINED status bills
                // These are the orders that are part of this combined bill
                List<Order> tableOrders = orderRepository.findCompletedOrdersByTableId(primaryOrder.getTable().getId());
                // Filter to get orders that have COMBINED status bills (reference bills)
                // These are orders created after the last bill and included in this bill
                allOrders = tableOrders.stream()
                    .filter(order -> {
                        Optional<Bill> orderBill = billRepository.findByOrderId(order.getId());
                        if (orderBill.isPresent() && "COMBINED".equals(orderBill.get().getPaymentStatus())) {
                            // Check if this order's bill was generated around the same time as the combined bill
                            // (within 1 minute to account for processing time)
                            long timeDiff = Math.abs(java.time.Duration.between(
                                orderBill.get().getGeneratedAt(), 
                                bill.getGeneratedAt()
                            ).toMinutes());
                            return timeDiff <= 1; // Orders billed within 1 minute are part of this combined bill
                        }
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());
                // Also include the primary order
                if (!allOrders.contains(primaryOrder)) {
                    allOrders.add(0, primaryOrder);
                }
            } else {
                allOrders.add(primaryOrder);
            }
            
            // Collect all items from all orders
            for (Order order : allOrders) {
                if (order.getItems() != null) {
                    allItems.addAll(order.getItems());
                }
            }
        } else {
            // Fallback to single order
            if (primaryOrder != null && primaryOrder.getItems() != null) {
                allItems.addAll(primaryOrder.getItems());
            }
        }
        
        invoice.append("========================================\n");
        invoice.append("          RESTAURANT INVOICE\n");
        invoice.append("========================================\n\n");
        
        invoice.append("Invoice ID: ").append(bill.getId()).append("\n");
        if (allOrders.size() > 1) {
            invoice.append("Orders: ").append(allOrders.size()).append(" orders combined\n");
            invoice.append("Order IDs: ");
            for (int i = 0; i < allOrders.size(); i++) {
                if (i > 0) invoice.append(", ");
                invoice.append(allOrders.get(i).getId().toString().substring(0, 8));
            }
            invoice.append("\n");
        } else if (primaryOrder != null) {
            invoice.append("Order ID: ").append(primaryOrder.getId()).append("\n");
        }
        invoice.append("Table: ").append(primaryOrder != null && primaryOrder.getTable() != null ? primaryOrder.getTable().getTableNumber() : "N/A").append("\n");
        invoice.append("Date: ").append(bill.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        // GST Details
        if (bill.getCompanyGstin() != null) {
            invoice.append("Company GSTIN: ").append(bill.getCompanyGstin()).append("\n");
        }
        if (bill.getCustomerGstin() != null) {
            invoice.append("Customer GSTIN: ").append(bill.getCustomerGstin()).append("\n");
        }
        if (bill.getPlaceOfSupply() != null) {
            invoice.append("Place of Supply: ").append(bill.getPlaceOfSupply()).append("\n");
        }
        invoice.append("\n");
        
        invoice.append("----------------------------------------\n");
        invoice.append("Items:\n");
        invoice.append("----------------------------------------\n");
        invoice.append(String.format("%-20s %6s %5s %10s %10s\n", "Item", "HSN", "Qty", "Rate", "Amount"));
        invoice.append("----------------------------------------\n");
        
        // Group items by menu item and sum quantities
        java.util.Map<UUID, OrderItem> itemMap = new java.util.HashMap<>();
        for (OrderItem item : allItems) {
            UUID menuItemId = item.getMenuItem().getId();
            if (itemMap.containsKey(menuItemId)) {
                OrderItem existing = itemMap.get(menuItemId);
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                existing.setPrice(existing.getPrice() + item.getPrice());
            } else {
                // Create a copy to avoid modifying the original
                OrderItem copy = OrderItem.builder()
                    .menuItem(item.getMenuItem())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build();
                itemMap.put(menuItemId, copy);
            }
        }
        
        // Display all items
        for (OrderItem item : itemMap.values()) {
            String hsnCode = item.getMenuItem().getHsnCode() != null ? item.getMenuItem().getHsnCode() : "N/A";
            double unitPrice = item.getMenuItem().getPrice();
            invoice.append(String.format("%-20s %6s %5d %10.2f %10.2f\n",
                item.getMenuItem().getName(),
                hsnCode,
                item.getQuantity(),
                unitPrice,
                item.getPrice()));
        }
        
        invoice.append("----------------------------------------\n");
        invoice.append(String.format("Subtotal:              %.2f\n", bill.getTotalAmount()));
        if (bill.getDiscountAmount() > 0) {
            invoice.append(String.format("Discount:              -%.2f\n", bill.getDiscountAmount()));
        }
        
        // GST Breakdown
        if (bill.getIsInterState() != null && bill.getIsInterState()) {
            // Inter-state: IGST
            if (bill.getIgst() != null && bill.getIgst() > 0) {
                invoice.append(String.format("IGST:                  %.2f\n", bill.getIgst()));
            }
        } else {
            // Intra-state: CGST + SGST
            if (bill.getCgst() != null && bill.getCgst() > 0) {
                invoice.append(String.format("CGST:                  %.2f\n", bill.getCgst()));
            }
            if (bill.getSgst() != null && bill.getSgst() > 0) {
                invoice.append(String.format("SGST:                  %.2f\n", bill.getSgst()));
            }
        }
        invoice.append(String.format("Total Tax:             %.2f\n", bill.getTax()));
        invoice.append("----------------------------------------\n");
        invoice.append(String.format("Grand Total:           %.2f\n", bill.getGrandTotal()));
        invoice.append("========================================\n");
        invoice.append("        Thank you for your visit!\n");
        invoice.append("========================================\n");
        
        return invoice.toString();
    }

    public byte[] generateInvoicePdf(Bill bill) throws Exception {
        Order primaryOrder = bill.getOrder();
        
        // Get all orders for combined bills
        List<Order> allOrders = new ArrayList<>();
        List<OrderItem> allItems = new ArrayList<>();
        
        if (primaryOrder != null && primaryOrder.getTable() != null) {
            if ("COMBINED_BILL".equals(bill.getPaymentStatus())) {
                List<Order> tableOrders = orderRepository.findCompletedOrdersByTableId(primaryOrder.getTable().getId());
                allOrders = tableOrders.stream()
                    .filter(order -> {
                        Optional<Bill> orderBill = billRepository.findByOrderId(order.getId());
                        if (orderBill.isPresent() && "COMBINED".equals(orderBill.get().getPaymentStatus())) {
                            // Check if this order's bill was generated around the same time as the combined bill
                            long timeDiff = Math.abs(java.time.Duration.between(
                                orderBill.get().getGeneratedAt(), 
                                bill.getGeneratedAt()
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
            
            for (Order order : allOrders) {
                if (order.getItems() != null) {
                    allItems.addAll(order.getItems());
                }
            }
        } else {
            if (primaryOrder != null && primaryOrder.getItems() != null) {
                allItems.addAll(primaryOrder.getItems());
            }
        }
        
        // Group items by menu item
        java.util.Map<UUID, OrderItem> itemMap = new java.util.HashMap<>();
        for (OrderItem item : allItems) {
            UUID menuItemId = item.getMenuItem().getId();
            if (itemMap.containsKey(menuItemId)) {
                OrderItem existing = itemMap.get(menuItemId);
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                existing.setPrice(existing.getPrice() + item.getPrice());
            } else {
                OrderItem copy = OrderItem.builder()
                    .menuItem(item.getMenuItem())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build();
                itemMap.put(menuItemId, copy);
            }
        }
        
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            
            try {
                float yPosition = 750;
                float leftMargin = 50;
                float lineHeight = 20;
                
                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(leftMargin, yPosition);
                cs.showText("RESTAURANT INVOICE");
                cs.endText();
                
                yPosition -= 30;
                
                // Invoice Details
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(leftMargin, yPosition);
                cs.showText("Invoice ID: " + bill.getId());
                yPosition -= lineHeight;
                cs.newLineAtOffset(0, -lineHeight);
                if (allOrders.size() > 1) {
                    cs.showText("Orders: " + allOrders.size() + " orders combined");
                    yPosition -= lineHeight;
                    cs.newLineAtOffset(0, -lineHeight);
                } else if (primaryOrder != null) {
                    cs.showText("Order ID: " + primaryOrder.getId());
                    yPosition -= lineHeight;
                    cs.newLineAtOffset(0, -lineHeight);
                }
                cs.showText("Table: " + (primaryOrder != null && primaryOrder.getTable() != null ? primaryOrder.getTable().getTableNumber() : "N/A"));
                yPosition -= lineHeight;
                cs.newLineAtOffset(0, -lineHeight);
                cs.showText("Date: " + bill.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                if (bill.getCompanyGstin() != null) {
                    yPosition -= lineHeight;
                    cs.newLineAtOffset(0, -lineHeight);
                    cs.showText("Company GSTIN: " + bill.getCompanyGstin());
                }
                if (bill.getCustomerGstin() != null) {
                    yPosition -= lineHeight;
                    cs.newLineAtOffset(0, -lineHeight);
                    cs.showText("Customer GSTIN: " + bill.getCustomerGstin());
                }
                cs.endText();
                
                yPosition -= 30;
                
                // Items Header
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(leftMargin, yPosition);
                cs.showText("Items:");
                cs.endText();
                
                yPosition -= lineHeight;
                
                // Items List
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(leftMargin, yPosition);
                
                if (!itemMap.isEmpty()) {
                    for (OrderItem item : itemMap.values()) {
                        if (yPosition < 100) {
                            // Need new page
                            cs.endText();
                            cs.close();
                            PDPage newPage = new PDPage();
                            doc.addPage(newPage);
                            cs = new PDPageContentStream(doc, newPage);
                            yPosition = 750;
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 10);
                            cs.newLineAtOffset(leftMargin, yPosition);
                        }
                        
                        String itemName = item.getMenuItem() != null ? item.getMenuItem().getName() : "Unknown Item";
                        String hsnCode = (item.getMenuItem() != null && item.getMenuItem().getHsnCode() != null) 
                            ? item.getMenuItem().getHsnCode() : "N/A";
                        double itemPrice = item.getMenuItem() != null ? item.getMenuItem().getPrice() : item.getPrice();
                        String itemLine = String.format("%s (HSN: %s) - %d x Rs%.2f = Rs%.2f",
                            itemName,
                            hsnCode,
                            item.getQuantity(),
                            itemPrice,
                            item.getPrice());
                        cs.showText(itemLine);
                        yPosition -= lineHeight;
                        cs.newLineAtOffset(0, -lineHeight);
                    }
                }
                cs.endText();
                
                yPosition -= 20;
                
                // Totals
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(leftMargin, yPosition);
                cs.showText("Subtotal: Rs" + String.format("%.2f", bill.getTotalAmount()));
                yPosition -= lineHeight;
                cs.newLineAtOffset(0, -lineHeight);
                if (bill.getDiscountAmount() > 0) {
                    cs.showText("Discount: -Rs" + String.format("%.2f", bill.getDiscountAmount()));
                    yPosition -= lineHeight;
                    cs.newLineAtOffset(0, -lineHeight);
                }
                
                // GST Breakdown
                if (bill.getIsInterState() != null && bill.getIsInterState()) {
                    if (bill.getIgst() != null && bill.getIgst() > 0) {
                        cs.showText("IGST: Rs" + String.format("%.2f", bill.getIgst()));
                        yPosition -= lineHeight;
                        cs.newLineAtOffset(0, -lineHeight);
                    }
                } else {
                    if (bill.getCgst() != null && bill.getCgst() > 0) {
                        cs.showText("CGST: Rs" + String.format("%.2f", bill.getCgst()));
                        yPosition -= lineHeight;
                        cs.newLineAtOffset(0, -lineHeight);
                    }
                    if (bill.getSgst() != null && bill.getSgst() > 0) {
                        cs.showText("SGST: Rs" + String.format("%.2f", bill.getSgst()));
                        yPosition -= lineHeight;
                        cs.newLineAtOffset(0, -lineHeight);
                    }
                }
                cs.showText("Total Tax: Rs" + String.format("%.2f", bill.getTax()));
                yPosition -= lineHeight;
                cs.newLineAtOffset(0, -lineHeight);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.showText("Grand Total: Rs" + String.format("%.2f", bill.getGrandTotal()));
                cs.endText();
                
                // Footer
                yPosition -= 40;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.newLineAtOffset(leftMargin, yPosition);
                cs.showText("Thank you for your visit!");
                cs.endText();
            } finally {
                if (cs != null) {
                    cs.close();
                }
            }

            doc.save(out);
            return out.toByteArray();
        }
    }
}

