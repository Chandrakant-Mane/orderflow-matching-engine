package com.orderflow.matching_engine.controller;

import com.orderflow.matching_engine.api.OrderRequest;
import com.orderflow.matching_engine.engine.OrderEvent;
import com.orderflow.matching_engine.model.Order;
import com.orderflow.matching_engine.model.Trade;
import com.orderflow.matching_engine.model.User;
import com.orderflow.matching_engine.repository.OrderRepository;
import com.orderflow.matching_engine.repository.TradeRepository;
import com.orderflow.matching_engine.repository.UserRepository;
import com.orderflow.matching_engine.service.RiskCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final RiskCheckService riskCheckService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderController(
            RiskCheckService riskCheckService,
            OrderRepository orderRepository,
            UserRepository userRepository,
            TradeRepository tradeRepository,
            KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.riskCheckService = riskCheckService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest incomingRequest, Authentication authentication) {

        // 1. Strict Identity Check (Checkpoint 1)
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized access. Missing token."));
        }
        String authenticatedUsername = authentication.getName();

        if (!authenticatedUsername.equals(incomingRequest.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("error", "Identity mismatch. Token does not match payload."));
        }

        // 2. Locate User
        User user = userRepository.findByUsername(authenticatedUsername).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User profile not found in database."));
        }

        // 3. Clean mapping from DTO (OrderRequest) to Entity (Order)
        Order newOrder = new Order();
        newOrder.setUsername(authenticatedUsername);
        newOrder.setClientOrderId(incomingRequest.getClientOrderId());
        newOrder.setSymbol(incomingRequest.getSymbol() != null ? incomingRequest.getSymbol() : "BTC-USD");
        newOrder.setSide(incomingRequest.getSide());
        newOrder.setPrice(incomingRequest.getPrice());
        newOrder.setQuantity(incomingRequest.getQuantity());

        try {
            // 4. Run Pre-Trade Risk Check
            riskCheckService.validateAndLockFunds(newOrder, user);

            // 5. Map to OrderEvent (using high-precision BigDecimal)
            OrderEvent orderEvent = new OrderEvent();
            orderEvent.set(
                    newOrder.getId(),
                    newOrder.getSymbol(),
                    newOrder.getPrice(),
                    newOrder.getQuantity(),
                    newOrder.getSide());

            // 6. Publish to Kafka
            kafkaTemplate.send("validated-orders", newOrder.getSymbol(), orderEvent);

            // 7. Brief pause to allow the local Kafka matching consumer to settle the trade
            try {
                Thread.sleep(60);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // 8. Re-fetch the fresh user state from PostgreSQL to get post-match balances
            User updatedUser = userRepository.findByUsername(authenticatedUsername).orElse(user);

            return ResponseEntity.ok(Map.of(
                    "status", "ACCEPTED",
                    "orderId", newOrder.getId(),
                    "message", "Order passed risk check and submitted to matching engine",
                    "updatedBalanceUsd", updatedUser.getBalanceUsd(),
                    "updatedBalanceBtc", updatedUser.getBalanceBtc()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "REJECTED",
                    "reason", e.getMessage()));
        }
    }

    // 9. Profile sync endpoint to fetch live wallet balances on demand
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "balanceUsd", user.getBalanceUsd(),
                "balanceBtc", user.getBalanceBtc()));
    }

    // 10. Fetch user's active/waiting orders
    @GetMapping("/me/active")
    public ResponseEntity<?> getMyActiveOrders(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        List<Order> activeOrders = orderRepository.findByUsernameAndStatus(
                authentication.getName(),
                Order.OrderStatus.WAITING);
        return ResponseEntity.ok(activeOrders);
    }

    // 11. Fetch user's trade history
    @GetMapping("/me/trades")
    public ResponseEntity<?> getMyTradeHistory(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        // Fetch the DTO projection directly from the database
        List<TradeRepository.TradeHistoryDto> myTrades = tradeRepository
                .findTradeHistoryByUsername(authentication.getName());

        return ResponseEntity.ok(myTrades);
    }


    // 12. Cancel an active order
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String username = authentication.getName();
        Order order = orderRepository.findById(id).orElse(null);

        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Order not found"));
        }

        // Security check: only the owner can cancel their order
        if (!order.getUsername().equals(username)) {
            return ResponseEntity.status(403).body(Map.of("error", "You do not have permission to cancel this order"));
        }

        // Can only cancel waiting orders
        if (order.getStatus() != Order.OrderStatus.WAITING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only WAITING orders can be cancelled"));
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        try {
            // 1. Refund the user and mark order as CANCELLED in DB
            riskCheckService.cancelAndRefund(order, user);

            // 2. Create the cancel event for the Disruptor engine
            OrderEvent cancelEvent = new OrderEvent();
            cancelEvent.set(
                    order.getId(),
                    order.getSymbol(),
                    order.getPrice(),
                    order.getQuantity(),
                    order.getSide());

            // Critical: tell the engine to remove it, not match it
            cancelEvent.isCancel = true;

            // 3. Publish to Kafka
            kafkaTemplate.send("validated-orders", order.getSymbol(), cancelEvent);

            return ResponseEntity.ok(Map.of(
                    "message", "Order cancelled successfully",
                    "orderId", order.getId()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}