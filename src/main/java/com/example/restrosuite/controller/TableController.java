package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Outlet;
import com.example.restrosuite.entity.TableEntity;
import com.example.restrosuite.repository.OutletRepository;
import com.example.restrosuite.repository.TableRepository;
import com.example.restrosuite.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
public class TableController {

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private OutletRepository outletRepository;

    @Autowired
    private QrCodeService qrCodeService;

    @GetMapping
    public List<TableEntity> getAllTables() {
        return tableRepository.findAll();
    }

    @PostMapping
    public TableEntity createTable(@RequestBody Map<String, Object> payload) {
        TableEntity table = new TableEntity();
        table.setTableNumber(payload.get("tableNumber").toString());
        table.setOccupied(false);
        if (payload.containsKey("capacity") && payload.get("capacity") != null) {
            table.setCapacity(Integer.parseInt(payload.get("capacity").toString()));
        }
        if (payload.containsKey("location") && payload.get("location") != null) {
            table.setLocation(payload.get("location").toString());
        }
        
        // Set outlet if provided
        if (payload.containsKey("outletId") && payload.get("outletId") != null) {
            UUID outletId = UUID.fromString(payload.get("outletId").toString());
            Outlet outlet = outletRepository.findById(outletId)
                    .orElseThrow(() -> new RuntimeException("Outlet not found"));
            table.setOutlet(outlet);
        }
        
        TableEntity savedTable = tableRepository.save(table);
        
        // Generate QR code URL for the table
        try {
            String qrCodeUrl = qrCodeService.generateQrCodeUrl(savedTable.getId());
            savedTable.setQrCodeUrl(qrCodeUrl);
            savedTable = tableRepository.save(savedTable);
        } catch (Exception e) {
            // Log error but don't fail table creation
            System.err.println("Failed to generate QR code URL: " + e.getMessage());
        }
        
        return savedTable;
    }

    @PutMapping("/{id}")
    public TableEntity updateTable(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        TableEntity existing = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        existing.setTableNumber(payload.get("tableNumber").toString());
        existing.setOccupied(Boolean.parseBoolean(payload.get("occupied").toString()));
        if (payload.containsKey("capacity") && payload.get("capacity") != null) {
            existing.setCapacity(Integer.parseInt(payload.get("capacity").toString()));
        }
        if (payload.containsKey("location") && payload.get("location") != null) {
            existing.setLocation(payload.get("location").toString());
        }
        
        // Update outlet if provided
        if (payload.containsKey("outletId") && payload.get("outletId") != null) {
            UUID outletId = UUID.fromString(payload.get("outletId").toString());
            Outlet outlet = outletRepository.findById(outletId)
                    .orElseThrow(() -> new RuntimeException("Outlet not found"));
            existing.setOutlet(outlet);
        } else if (payload.containsKey("outletId") && payload.get("outletId") == null) {
            existing.setOutlet(null);
        }
        
        // Regenerate QR code URL if needed
        if (existing.getQrCodeUrl() == null || existing.getQrCodeUrl().isEmpty()) {
            try {
                String qrCodeUrl = qrCodeService.generateQrCodeUrl(existing.getId());
                existing.setQrCodeUrl(qrCodeUrl);
            } catch (Exception e) {
                System.err.println("Failed to generate QR code URL: " + e.getMessage());
            }
        }
        
        return tableRepository.save(existing);
    }

    @GetMapping("/{id}/qr-code")
    public ResponseEntity<Map<String, Object>> getQrCode(@PathVariable UUID id) {
        try {
            TableEntity table = tableRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Always regenerate QR code URL to ensure it uses the correct frontend URL
            // This fixes any existing tables that might have old backend URLs stored
            String qrCodeUrl = qrCodeService.generateQrCodeUrl(table.getId());
            table.setQrCodeUrl(qrCodeUrl);
            table = tableRepository.save(table);
            
            String qrCodeBase64 = qrCodeService.generateQrCodeBase64(table.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("qrCodeImage", "data:image/png;base64," + qrCodeBase64);
            response.put("qrCodeUrl", qrCodeUrl);
            response.put("tableId", table.getId());
            response.put("tableNumber", table.getTableNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate QR code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{id}/qr-code/download")
    public ResponseEntity<byte[]> downloadQrCode(@PathVariable UUID id) {
        try {
            TableEntity table = tableRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            String qrCodeBase64 = qrCodeService.generateQrCodeBase64(table.getId());
            byte[] imageBytes = Base64.getDecoder().decode(qrCodeBase64);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", "table-" + table.getTableNumber() + "-qr.png");
            
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public String deleteTable(@PathVariable UUID id) {
        tableRepository.deleteById(id);
        return "Table deleted!";
    }

}

