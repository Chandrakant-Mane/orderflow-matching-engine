package com.orderflow.matching_engine.engine;

import java.math.BigDecimal;

public class OrderEvent {
    public long orderId;
    public String symbol;
    public BigDecimal price;
    public BigDecimal quantity;
    public String side;

    // NEW: Flag to tell the Disruptor to remove this order from the book
    public boolean isCancel = false;

    public void set(long orderId, String symbol, BigDecimal price, BigDecimal quantity, String side) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.isCancel = false; // Default to false for normal orders
    }

    public boolean isBuy() {
        return side != null && side.toUpperCase().startsWith("B");
    }
}