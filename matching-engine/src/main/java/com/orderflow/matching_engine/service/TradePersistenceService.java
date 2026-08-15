package com.orderflow.matching_engine.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import com.orderflow.matching_engine.model.Trade;
import com.orderflow.matching_engine.repository.TradeRepository;
import java.time.Duration;

@Service
public class TradePersistenceService {

    private final TradePublisher tradePublisher;
    private final TradeRepository tradeRepository;

    public TradePersistenceService(TradePublisher tradePublisher, TradeRepository tradeRepository) {
        this.tradePublisher = tradePublisher;
        this.tradeRepository = tradeRepository;
    }

    @PostConstruct
    public void init() {
        tradePublisher.getTradeStream()
                .map(Trade::new)
                .bufferTimeout(100, Duration.ofSeconds(1))
                .subscribe(trades -> {
                    if (!trades.isEmpty()) {
                        tradeRepository.saveAll(trades);
                        System.out.println("Persisted " + trades.size() + " trades to PostgreSQL.");
                    }
                });
    }
}