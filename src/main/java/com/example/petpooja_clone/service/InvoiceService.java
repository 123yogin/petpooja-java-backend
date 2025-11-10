package com.example.petpooja_clone.service;

import com.example.petpooja_clone.entity.Bill;
import com.example.petpooja_clone.entity.Order;
import com.example.petpooja_clone.entity.OrderItem;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceService {

    public String generateTextInvoice(Bill bill) {
        Order order = bill.getOrder();
        StringBuilder invoice = new StringBuilder();
        
        invoice.append("========================================\n");
        invoice.append("          RESTAURANT INVOICE\n");
        invoice.append("========================================\n\n");
        
        invoice.append("Invoice ID: ").append(bill.getId()).append("\n");
        invoice.append("Order ID: ").append(order.getId()).append("\n");
        invoice.append("Table: ").append(order.getTable() != null ? order.getTable().getTableNumber() : "N/A").append("\n");
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
        
        for (OrderItem item : order.getItems()) {
            String hsnCode = item.getMenuItem().getHsnCode() != null ? item.getMenuItem().getHsnCode() : "N/A";
            invoice.append(String.format("%-20s %6s %5d %10.2f %10.2f\n",
                item.getMenuItem().getName(),
                hsnCode,
                item.getQuantity(),
                item.getMenuItem().getPrice(),
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
        Order order = bill.getOrder();
        
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
                cs.showText("Order ID: " + order.getId());
                yPosition -= lineHeight;
                cs.newLineAtOffset(0, -lineHeight);
                cs.showText("Table: " + (order.getTable() != null ? order.getTable().getTableNumber() : "N/A"));
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
                
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
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

