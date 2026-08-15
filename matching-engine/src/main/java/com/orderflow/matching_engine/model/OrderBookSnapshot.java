package com.orderflow.matching_engine.model;


import java.util.List;

import com.orderflow.matching_engine.engine.PriceLevel;

public class OrderBookSnapshot {
    public String symbol;
    public List<PriceLevel> bids;
    public List<PriceLevel> asks;
    public long timestamp;

    public OrderBookSnapshot(String symbol, List<PriceLevel> bids, List<PriceLevel> asks) {
        this.symbol = symbol;
        this.bids = bids;
        this.asks = asks;
        this.timestamp = System.currentTimeMillis();
    }
}