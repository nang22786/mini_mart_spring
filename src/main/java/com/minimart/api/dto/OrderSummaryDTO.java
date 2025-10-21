package com.minimart.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummaryDTO {
    private Long id;
    private Long userId;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private Integer itemCount;
    private Long addressId;
    private LocalDateTime payDate;  // ✅ NEW!

    // Constructors
    public OrderSummaryDTO() {}

    public OrderSummaryDTO(Long id, Long userId, String status, BigDecimal amount,
                          LocalDateTime createdAt, Integer itemCount, Long addressId,
                          LocalDateTime payDate) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.amount = amount;
        this.createdAt = createdAt;
        this.itemCount = itemCount;
        this.addressId = addressId;
        this.payDate = payDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public LocalDateTime getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDateTime payDate) {
        this.payDate = payDate;
    }
}