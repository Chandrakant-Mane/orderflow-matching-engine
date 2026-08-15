package com.orderflow.matching_engine.api;

import java.math.BigDecimal;

public class OrderRequest {
    public String clientOrderId;
    public String username;
    public String symbol;
    public BigDecimal price;
    public BigDecimal quantity;
    public String side;

    public OrderRequest() {
    }

    // Getters
    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getUsername() {
        return username;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getSide() {
        return side;
    }
}