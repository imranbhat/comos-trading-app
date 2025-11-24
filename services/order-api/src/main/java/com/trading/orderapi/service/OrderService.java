package com.trading.orderapi.service;

import com.trading.orderapi.dto.CreateOrderRequest;
import com.trading.orderapi.model.OrderCommand;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderService {

    public OrderCommand createOrderCommand(CreateOrderRequest request) {
        OrderCommand command = new OrderCommand();
        command.setOrderId(UUID.randomUUID().toString());
        command.setSymbol(request.getSymbol());
        command.setSide(request.getSide());
        command.setQuantity(request.getQuantity());
        command.setPrice(request.getPrice());
        command.setOrderType(request.getOrderType());
        return command;
    }
}

