package com.trading.oms.model;

import java.time.Instant;

public class Order {
    private String orderId;
    private String symbol;
    private String side;
    private Integer quantity;
    private Integer filledQuantity;
    private Double price;
    private String orderType;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public Order() {
    }

    public Order(OrderCommand command) {
        this.orderId = command.getOrderId();
        this.symbol = command.getSymbol();
        this.side = command.getSide();
        this.quantity = command.getQuantity();
        this.price = command.getPrice();
        this.orderType = command.getOrderType();
        this.status = "PENDING";
        this.filledQuantity = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(Integer filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

