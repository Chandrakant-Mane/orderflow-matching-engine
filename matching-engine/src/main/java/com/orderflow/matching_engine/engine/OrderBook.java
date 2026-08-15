package com.orderflow.matching_engine.engine;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.orderflow.matching_engine.model.OrderBookSnapshot;
import com.orderflow.matching_engine.model.TradeEvent;
import com.orderflow.matching_engine.service.TradePublisher;
import com.orderflow.matching_engine.service.BookPublisher;

public class OrderBook {

    private final String symbol;
    private final TradePublisher tradePublisher;
    private final BookPublisher bookPublisher;

    private final TreeMap<BigDecimal, Queue<OrderEvent>> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<BigDecimal, Queue<OrderEvent>> asks = new TreeMap<>();

    public OrderBook(String symbol, TradePublisher tradePublisher, BookPublisher bookPublisher) {
        this.symbol = symbol;
        this.tradePublisher = tradePublisher;
        this.bookPublisher = bookPublisher;
    }

    public void processOrder(OrderEvent incoming) {
        // NEW: Intercept cancellations before they match
        if (incoming.isCancel) {
            cancelOrder(incoming);
            return;
        }

        if (incoming.isBuy()) {
            matchBuyOrder(incoming);
        } else {
            matchSellOrder(incoming);
        }

        broadcastBookSnapshot();
    }

    // NEW: Removes the resting order from the L2 Book
    private void cancelOrder(OrderEvent cancelEvent) {
        TreeMap<BigDecimal, Queue<OrderEvent>> bookSide = cancelEvent.isBuy() ? bids : asks;
        Queue<OrderEvent> queue = bookSide.get(cancelEvent.price);

        if (queue != null) {
            // Remove the exact order ID from the queue
            boolean removed = queue.removeIf(o -> o.orderId == cancelEvent.orderId);
            if (removed) {
                System.out.printf("  [ORDER CANCELLED] Removed %s from book | ID: %d | Price: %.2f%n",
                        cancelEvent.side, cancelEvent.orderId, cancelEvent.price);

                // Clean up empty price levels
                if (queue.isEmpty()) {
                    bookSide.remove(cancelEvent.price);
                }
            }
        }
        broadcastBookSnapshot();
    }

    private void matchBuyOrder(OrderEvent buyOrder) {
        Iterator<Map.Entry<BigDecimal, Queue<OrderEvent>>> askIterator = asks.entrySet().iterator();

        while (askIterator.hasNext() && buyOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
            Map.Entry<BigDecimal, Queue<OrderEvent>> entry = askIterator.next();
            BigDecimal bestAskPrice = entry.getKey();

            if (buyOrder.price.compareTo(bestAskPrice) < 0) {
                break;
            }

            Queue<OrderEvent> askQueue = entry.getValue();

            while (!askQueue.isEmpty() && buyOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
                OrderEvent restingAsk = askQueue.peek();
                BigDecimal executedQty = buyOrder.quantity.min(restingAsk.quantity);

                buyOrder.quantity = buyOrder.quantity.subtract(executedQty);
                restingAsk.quantity = restingAsk.quantity.subtract(executedQty);

                System.out.printf(
                        "  [TRADE EXECUTED] %s | Price: %.2f | Qty: %.4f | Buyer Order: %d | Seller Order: %d%n",
                        symbol, bestAskPrice, executedQty, buyOrder.orderId, restingAsk.orderId);

                tradePublisher.publishTrade(
                        new TradeEvent(symbol, bestAskPrice, executedQty, buyOrder.orderId, restingAsk.orderId));

                if (restingAsk.quantity.compareTo(BigDecimal.ZERO) == 0) {
                    askQueue.poll();
                }
            }

            if (askQueue.isEmpty()) {
                askIterator.remove();
            }
        }

        if (buyOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
            bids.computeIfAbsent(buyOrder.price, k -> new LinkedList<>()).add(cloneEvent(buyOrder));
            System.out.printf("  [ORDER RESTING] Placed BUY on book | ID: %d | Qty Remaining: %.4f | Price: %.2f%n",
                    buyOrder.orderId, buyOrder.quantity, buyOrder.price);
        }
    }

    private void matchSellOrder(OrderEvent sellOrder) {
        Iterator<Map.Entry<BigDecimal, Queue<OrderEvent>>> bidIterator = bids.entrySet().iterator();

        while (bidIterator.hasNext() && sellOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
            Map.Entry<BigDecimal, Queue<OrderEvent>> entry = bidIterator.next();
            BigDecimal bestBidPrice = entry.getKey();

            if (sellOrder.price.compareTo(bestBidPrice) > 0) {
                break;
            }

            Queue<OrderEvent> bidQueue = entry.getValue();

            while (!bidQueue.isEmpty() && sellOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
                OrderEvent restingBid = bidQueue.peek();
                BigDecimal executedQty = sellOrder.quantity.min(restingBid.quantity);

                sellOrder.quantity = sellOrder.quantity.subtract(executedQty);
                restingBid.quantity = restingBid.quantity.subtract(executedQty);

                System.out.printf(
                        "  [TRADE EXECUTED] %s | Price: %.2f | Qty: %.4f | Buyer Order: %d | Seller Order: %d%n",
                        symbol, bestBidPrice, executedQty, restingBid.orderId, sellOrder.orderId);

                tradePublisher.publishTrade(
                        new TradeEvent(symbol, bestBidPrice, executedQty, restingBid.orderId, sellOrder.orderId));

                if (restingBid.quantity.compareTo(BigDecimal.ZERO) == 0) {
                    bidQueue.poll();
                }
            }

            if (bidQueue.isEmpty()) {
                bidIterator.remove();
            }
        }

        if (sellOrder.quantity.compareTo(BigDecimal.ZERO) > 0) {
            asks.computeIfAbsent(sellOrder.price, k -> new LinkedList<>()).add(cloneEvent(sellOrder));
            System.out.printf("  [ORDER RESTING] Placed SELL on book | ID: %d | Qty Remaining: %.4f | Price: %.2f%n",
                    sellOrder.orderId, sellOrder.quantity, sellOrder.price);
        }
    }

    private void broadcastBookSnapshot() {
        List<PriceLevel> bidLevels = bids.entrySet().stream()
                .map(e -> {
                    BigDecimal totalQty = e.getValue().stream()
                            .map(o -> o.quantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new PriceLevel(e.getKey(), totalQty);
                })
                .collect(Collectors.toList());

        List<PriceLevel> askLevels = asks.entrySet().stream()
                .map(e -> {
                    BigDecimal totalQty = e.getValue().stream()
                            .map(o -> o.quantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new PriceLevel(e.getKey(), totalQty);
                })
                .collect(Collectors.toList());

        bookPublisher.publishSnapshot(new OrderBookSnapshot(symbol, bidLevels, askLevels));
    }

    private OrderEvent cloneEvent(OrderEvent original) {
        OrderEvent clone = new OrderEvent();
        clone.set(original.orderId, original.symbol, original.price, original.quantity, original.side);
        return clone;
    }
}