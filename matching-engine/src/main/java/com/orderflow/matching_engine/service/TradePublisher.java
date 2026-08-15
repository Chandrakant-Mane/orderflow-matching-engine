package com.orderflow.matching_engine.service;

import org.springframework.stereotype.Service;
import com.orderflow.matching_engine.model.TradeEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class TradePublisher {

    // Change from .replay() to .multicast() so it only streams live events going
    // forward
    private final Sinks.Many<TradeEvent> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void publishTrade(TradeEvent trade) {
        sink.tryEmitNext(trade);
    }

    public Flux<TradeEvent> getTradeStream() {
        return sink.asFlux();
    }
}