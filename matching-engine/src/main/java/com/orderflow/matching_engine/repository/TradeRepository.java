package com.orderflow.matching_engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.orderflow.matching_engine.model.Trade;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    // An interface to automatically map the SQL result into a JSON format for React
    interface TradeHistoryDto {
        Long getId();

        BigDecimal getPrice();

        BigDecimal getQuantity();

        String getBuyerUsername();

        String getSellerUsername();
    }

    // Explicit SQL query linking Trades to both the Buy Order and Sell Order
    @Query(value = "SELECT t.id as id, t.price as price, t.quantity as quantity, " +
            "ob.username as buyerUsername, os.username as sellerUsername " +
            "FROM trades t " +
            "JOIN orders ob ON t.buy_order_id = ob.id " +
            "JOIN orders os ON t.sell_order_id = os.id " +
            "WHERE ob.username = :username OR os.username = :username " +
            "ORDER BY t.executed_at DESC", nativeQuery = true)
    List<TradeHistoryDto> findTradeHistoryByUsername(@Param("username") String username);
}