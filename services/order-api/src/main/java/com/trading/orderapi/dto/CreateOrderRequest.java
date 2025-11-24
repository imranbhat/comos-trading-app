package com.trading.orderapi.dto;

import jakarta.validation.constraints.*;

public class CreateOrderRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotBlank(message = "Side is required")
    @Pattern(regexp = "BUY|SELL", message = "Side must be BUY or SELL")
    private String side;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;
    
    @NotBlank(message = "Order type is required")
    @Pattern(regexp = "LIMIT|MARKET", message = "Order type must be LIMIT or MARKET")
    private String orderType;

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
}

