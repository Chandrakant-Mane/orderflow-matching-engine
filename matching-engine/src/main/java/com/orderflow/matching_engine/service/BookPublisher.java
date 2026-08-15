package com.orderflow.matching_engine.service;

import org.springframework.stereotype.Service;
import com.orderflow.matching_engine.model.OrderBookSnapshot;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class BookPublisher {
    private final Sinks.Many<OrderBookSnapshot> sink = Sinks.many().replay().latest();

    public void publishSnapshot(OrderBookSnapshot snapshot) {
        sink.tryEmitNext(snapshot);
    }

    public Flux<OrderBookSnapshot> getBookStream() {
        return sink.asFlux();
    }
}