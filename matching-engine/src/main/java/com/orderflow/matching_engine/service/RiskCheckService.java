package com.orderflow.matching_engine.service;

import com.orderflow.matching_engine.model.Order;
import com.orderflow.matching_engine.model.User;
import com.orderflow.matching_engine.repository.OrderRepository;
import com.orderflow.matching_engine.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RiskCheckService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public RiskCheckService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void validateAndLockFunds(Order order, User user) {
        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid price: Must be greater than 0");
        }
        if (order.getQuantity() == null || order.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid quantity: Must be greater than 0");
        }

        String side = order.getSide() != null ? order.getSide().toUpperCase() : "";

        if (side.startsWith("B")) {
            BigDecimal totalRequiredUsd = order.getPrice().multiply(order.getQuantity());
            if (user.getBalanceUsd().compareTo(totalRequiredUsd) < 0) {
                throw new IllegalStateException(String.format(
                        "Risk Check Failed: Insufficient USD balance. Required: $%.2f, Available: $%.2f",
                        totalRequiredUsd, user.getBalanceUsd()));
            }
            user.setBalanceUsd(user.getBalanceUsd().subtract(totalRequiredUsd));

        } else if (side.startsWith("S")) {
            if (user.getBalanceBtc().compareTo(order.getQuantity()) < 0) {
                throw new IllegalStateException(String.format(
                        "Risk Check Failed: Insufficient BTC balance. Required: %.4f BTC, Available: %.4f BTC",
                        order.getQuantity(), user.getBalanceBtc()));
            }
            user.setBalanceBtc(user.getBalanceBtc().subtract(order.getQuantity()));

        } else {
            throw new IllegalArgumentException("Invalid side: Must be 'BUY' or 'SELL'");
        }

        userRepository.save(user);

        order.setStatus(Order.OrderStatus.WAITING);
        if (order.getFilledQuantity() == null) {
            order.setFilledQuantity(BigDecimal.ZERO);
        }
        orderRepository.save(order);
    }

    // NEW: Unlocks funds for the remaining quantity of an order and marks it
    // CANCELLED
    @Transactional
    public void cancelAndRefund(Order order, User user) {
        if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.FILLED) {
            throw new IllegalStateException("Order is already fully executed or cancelled.");
        }

        // Only refund the portion of the order that hasn't executed yet
        BigDecimal remainingQty = order.getQuantity().subtract(order.getFilledQuantity());

        String side = order.getSide().toUpperCase();
        if (side.startsWith("B")) {
            BigDecimal refundUsd = order.getPrice().multiply(remainingQty);
            user.setBalanceUsd(user.getBalanceUsd().add(refundUsd));
        } else if (side.startsWith("S")) {
            user.setBalanceBtc(user.getBalanceBtc().add(remainingQty));
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        userRepository.save(user);
        orderRepository.save(order);
    }
}