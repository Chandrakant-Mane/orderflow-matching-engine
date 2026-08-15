package com.orderflow.matching_engine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "buy_order_id", nullable = false)
    private Long buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private Long sellOrderId;

    @Column(name = "executed_at", nullable = false)
    private Long executedAt;

    // Constructor mapping to your existing TradeEvent
    public Trade(TradeEvent event) {
        this.symbol = event.symbol;
        this.price = event.price;
        this.quantity = event.quantity;
        this.buyOrderId = event.buyerOrderId;
        this.sellOrderId = event.sellerOrderId;
        this.executedAt = event.timestamp;
    }
}