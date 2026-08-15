package com.orderflow.matching_engine.model;

import java.math.BigDecimal;

public class TradeEvent {
    public String symbol;
    public BigDecimal price;
    public BigDecimal quantity;
    public long buyerOrderId;
    public long sellerOrderId;
    public long timestamp;

    public TradeEvent(String symbol, BigDecimal price, BigDecimal quantity, long buyerOrderId, long sellerOrderId) {
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.buyerOrderId = buyerOrderId;
        this.sellerOrderId = sellerOrderId;
        this.timestamp = System.currentTimeMillis();
    }
}