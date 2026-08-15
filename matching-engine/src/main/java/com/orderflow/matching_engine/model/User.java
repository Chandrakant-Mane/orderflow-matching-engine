package com.orderflow.matching_engine.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "balance_usd", nullable = false)
    private BigDecimal balanceUsd = BigDecimal.ZERO;

    @Column(name = "balance_btc", nullable = false)
    private BigDecimal balanceBtc = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public BigDecimal getBalanceUsd() {
        return balanceUsd;
    }

    public void setBalanceUsd(BigDecimal balanceUsd) {
        this.balanceUsd = balanceUsd;
    }

    public BigDecimal getBalanceBtc() {
        return balanceBtc;
    }

    public void setBalanceBtc(BigDecimal balanceBtc) {
        this.balanceBtc = balanceBtc;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}