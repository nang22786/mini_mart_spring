package com.minimart.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDTO {
    private Long id;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String paymentMethod;
    private String currency;
    private String screenshotPath;
    private String transactionId;
    private LocalDateTime transactionDate;
    private String status;
    private LocalDateTime payDate;
    private LocalDateTime createdAt;

    // Constructors
    public PaymentDTO() {}

    public PaymentDTO(Long id, Long orderId, Long userId, BigDecimal amount,
                     String paymentMethod, String currency, String screenshotPath,
                     String transactionId, LocalDateTime transactionDate, String status,
                     LocalDateTime payDate, LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.currency = currency;
        this.screenshotPath = screenshotPath;
        this.transactionId = transactionId;
        this.transactionDate = transactionDate;
        this.status = status;
        this.payDate = payDate;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDateTime payDate) {
        this.payDate = payDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}