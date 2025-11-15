package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Order;
import com.example.restrosuite.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Order createOrder(@RequestBody Map<String, Object> payload) {
        return orderService.createOrder(payload);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return orderService.updateOrderStatus(id, status);
    }

}

