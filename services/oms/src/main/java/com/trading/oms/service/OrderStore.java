package com.trading.oms.service;

import com.trading.oms.model.Order;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrderStore {
    private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();

    public boolean exists(String orderId) {
        return orders.containsKey(orderId);
    }

    public void save(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public Order get(String orderId) {
        return orders.get(orderId);
    }

    public void updateStatus(String orderId, String status) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(status);
            order.setUpdatedAt(java.time.Instant.now());
        }
    }

    public void updateFilledQuantity(String orderId, Integer filledQuantity) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setFilledQuantity(filledQuantity);
            order.setUpdatedAt(java.time.Instant.now());
        }
    }
}

