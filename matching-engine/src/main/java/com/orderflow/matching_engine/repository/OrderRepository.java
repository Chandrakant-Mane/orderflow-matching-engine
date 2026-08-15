package com.orderflow.matching_engine.repository;

import com.orderflow.matching_engine.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUsername(String username);

    List<Order> findByUsernameAndStatus(String username, Order.OrderStatus status);
}