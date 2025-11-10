package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Outlet;
import com.example.restrosuite.repository.OutletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/outlets")
public class OutletController {

    @Autowired
    private OutletRepository outletRepository;

    @GetMapping
    public List<Outlet> getAllOutlets() {
        return outletRepository.findAll();
    }

    @GetMapping("/active")
    public List<Outlet> getActiveOutlets() {
        return outletRepository.findByIsActiveTrue();
    }

    @GetMapping("/{id}")
    public Outlet getOutletById(@PathVariable UUID id) {
        return outletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outlet not found with ID: " + id));
    }

    @PostMapping
    public Outlet createOutlet(@RequestBody Outlet outlet) {
        // Check if outlet code already exists
        if (outlet.getCode() != null && outletRepository.findByCode(outlet.getCode()).isPresent()) {
            throw new RuntimeException("Outlet code already exists: " + outlet.getCode());
        }
        if (outlet.getIsActive() == null) {
            outlet.setIsActive(true);
        }
        return outletRepository.save(outlet);
    }

    @PutMapping("/{id}")
    public Outlet updateOutlet(@PathVariable UUID id, @RequestBody Outlet outletDetails) {
        Outlet outlet = outletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outlet not found with ID: " + id));

        outlet.setName(outletDetails.getName());
        outlet.setCode(outletDetails.getCode());
        outlet.setAddress(outletDetails.getAddress());
        outlet.setCity(outletDetails.getCity());
        outlet.setState(outletDetails.getState());
        outlet.setPincode(outletDetails.getPincode());
        outlet.setPhone(outletDetails.getPhone());
        outlet.setEmail(outletDetails.getEmail());
        outlet.setGstin(outletDetails.getGstin());
        outlet.setIsActive(outletDetails.getIsActive());
        outlet.setManagerName(outletDetails.getManagerName());
        outlet.setManagerPhone(outletDetails.getManagerPhone());

        return outletRepository.save(outlet);
    }

    @DeleteMapping("/{id}")
    public String deleteOutlet(@PathVariable UUID id) {
        outletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outlet not found with ID: " + id));
        outletRepository.deleteById(id);
        return "Outlet deleted successfully!";
    }
}

