package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Outlet;
import com.example.petpooja_clone.entity.TableEntity;
import com.example.petpooja_clone.repository.OutletRepository;
import com.example.petpooja_clone.repository.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        
        return tableRepository.save(table);
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
        
        return tableRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public String deleteTable(@PathVariable UUID id) {
        tableRepository.deleteById(id);
        return "Table deleted!";
    }

}

