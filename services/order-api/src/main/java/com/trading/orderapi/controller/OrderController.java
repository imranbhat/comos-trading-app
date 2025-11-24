package com.trading.orderapi.controller;

import com.trading.orderapi.dto.CreateOrderRequest;
import com.trading.orderapi.dto.CreateOrderResponse;
import com.trading.orderapi.model.OrderCommand;
import com.trading.orderapi.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private static final String TOPIC = "orders.commands";
    
    private final OrderService orderService;
    private final KafkaTemplate<String, OrderCommand> kafkaTemplate;

    public OrderController(OrderService orderService, KafkaTemplate<String, OrderCommand> kafkaTemplate) {
        this.orderService = orderService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderCommand command = orderService.createOrderCommand(request);
        kafkaTemplate.send(TOPIC, command.getOrderId(), command);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new CreateOrderResponse(
                    command.getOrderId(),
                    "PENDING",
                    "Order submitted successfully"
                ));
    }
}

