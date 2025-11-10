package com.example.restrosuite.controller;

import com.example.restrosuite.entity.Order;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/order-updates")
    @SendTo("/topic/orders")
    public Order broadcastOrderUpdate(Order order) {
        return order; // Broadcast updated order to all subscribers
    }
}

