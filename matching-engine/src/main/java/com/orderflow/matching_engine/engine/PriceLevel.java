package com.orderflow.matching_engine.engine;

import java.math.BigDecimal;

public class PriceLevel {
    public BigDecimal price;
    public BigDecimal quantity;

    public PriceLevel(BigDecimal price, BigDecimal quantity) {
        this.price = price;
        this.quantity = quantity;
    }
}