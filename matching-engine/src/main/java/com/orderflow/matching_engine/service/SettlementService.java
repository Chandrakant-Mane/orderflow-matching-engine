package com.orderflow.matching_engine.service;

import com.orderflow.matching_engine.model.Order;
import com.orderflow.matching_engine.model.TradeEvent;
import com.orderflow.matching_engine.model.User;
import com.orderflow.matching_engine.repository.OrderRepository;
import com.orderflow.matching_engine.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SettlementService {

    private final TradePublisher tradePublisher;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SettlementService self;

    // We use @Lazy so Spring injects a proxy instead of getting stuck in a cycle!
    public SettlementService(TradePublisher tradePublisher,
            UserRepository userRepository,
            OrderRepository orderRepository,
            @Lazy SettlementService self) {
        this.tradePublisher = tradePublisher;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.self = self;
    }

    @PostConstruct
    public void init() {
        // We can now safely subscribe using our lazy-loaded self-reference
        tradePublisher.getTradeStream().subscribe(self::processSettlementTransactionally);
    }

    @Transactional
    public void processSettlementTransactionally(TradeEvent trade) {
        Order buyerOrder = orderRepository.findById(trade.buyerOrderId).orElse(null);
        Order sellerOrder = orderRepository.findById(trade.sellerOrderId).orElse(null);

        if (buyerOrder != null && sellerOrder != null) {
            User buyer = userRepository.findByUsername(buyerOrder.getUsername()).orElse(null);
            User seller = userRepository.findByUsername(sellerOrder.getUsername()).orElse(null);

            // 1. Credit BTC to the Buyer
            if (buyer != null) {
                buyer.setBalanceBtc(buyer.getBalanceBtc().add(trade.quantity));
                userRepository.save(buyer);
            }

            // 2. Credit USD to the Seller
            if (seller != null) {
                BigDecimal usdEarnings = trade.price.multiply(trade.quantity);
                seller.setBalanceUsd(seller.getBalanceUsd().add(usdEarnings));
                userRepository.save(seller);
            }

            // 3. Update Order Statuses and Filled Quantities
            updateOrderProgress(buyerOrder, trade.quantity);
            updateOrderProgress(sellerOrder, trade.quantity);

            orderRepository.save(buyerOrder);
            orderRepository.save(sellerOrder);

            System.out.println("✅ Settlement & Database Sync Complete: Credited " + trade.quantity +
                    " BTC to " + buyerOrder.getUsername() + " and $" +
                    (trade.price.multiply(trade.quantity)) + " USD to " + sellerOrder.getUsername());
        }
    }

    // Helper method to calculate fill progress
    private void updateOrderProgress(Order order, BigDecimal executedQuantity) {
        BigDecimal currentFilled = order.getFilledQuantity() != null ? order.getFilledQuantity() : BigDecimal.ZERO;
        BigDecimal newFilled = currentFilled.add(executedQuantity);

        order.setFilledQuantity(newFilled);

        // If the filled quantity equals (or exceeds) the requested quantity, it's
        // FILLED
        if (newFilled.compareTo(order.getQuantity()) >= 0) {
            order.setStatus(Order.OrderStatus.FILLED);
        } else {
            order.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
        }
    }
}