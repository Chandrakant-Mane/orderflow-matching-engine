package com.orderflow.matching_engine.engine;

import com.lmax.disruptor.EventHandler;
import com.orderflow.matching_engine.service.BookPublisher;
import com.orderflow.matching_engine.service.TradePublisher;

public class MatchingEngine implements EventHandler<OrderEvent> {

    private final OrderBook btcOrderBook;

    public MatchingEngine(TradePublisher tradePublisher, BookPublisher bookPublisher) {
        this.btcOrderBook = new OrderBook("BTC-USD", tradePublisher, bookPublisher);
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        if (event == null || event.side == null) {
            return;
        }

        String sideFormatted = event.isBuy() ? "BUY" : "SELL";

        System.out.printf(">>> Ingesting Order #%d [%s %.2f x %.4f]%n",
                event.orderId, sideFormatted, event.price, event.quantity);

        btcOrderBook.processOrder(event);
    }
}