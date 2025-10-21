package com.minimart.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment", schema = "final")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    private BigDecimal amount;

    private String currency;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "screenshot_path")
    private String screenshotPath;

    // 🆕 NEW: Transaction ID from payment screenshot
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    // 🆕 NEW: Transaction date from payment screenshot
    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    // Constructors
    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.payDate = LocalDateTime.now();
    }

    public Payment(String paymentMethod, BigDecimal amount, String currency,
                   String status, Long orderId, Long userId) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.orderId = orderId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.payDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDateTime payDate) {
        this.payDate = payDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    // 🆕 NEW Getters/Setters for Transaction ID
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    // 🆕 NEW Getters/Setters for Transaction Date
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}