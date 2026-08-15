package com.orderflow.matching_engine.controller;

import com.orderflow.matching_engine.model.OrderBookSnapshot;
import com.orderflow.matching_engine.model.TradeEvent;
import com.orderflow.matching_engine.service.BookPublisher;
import com.orderflow.matching_engine.service.TradePublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StreamController {

    private final BookPublisher bookPublisher;
    private final TradePublisher tradePublisher;

    public StreamController(BookPublisher bookPublisher, TradePublisher tradePublisher) {
        this.bookPublisher = bookPublisher;
        this.tradePublisher = tradePublisher;
    }

    @GetMapping(value = "/book/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<OrderBookSnapshot> streamOrderBook() {
        return bookPublisher.getBookStream();
    }

    @GetMapping(value = "/trades/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TradeEvent> streamTrades() {
        return tradePublisher.getTradeStream();
    }
}