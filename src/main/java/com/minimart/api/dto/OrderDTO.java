package com.minimart.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDTO {
    private Long id;
    private String status;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private Long addressId;
    private List<OrderDetailDTO> items;
    private PaymentDTO payment;  // ✅ NEW!

    // Constructors
    public OrderDTO() {}

    public OrderDTO(Long id, String status, BigDecimal amount, 
                   LocalDateTime createdAt, LocalDateTime updatedAt, 
                   Long userId, Long addressId) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.addressId = addressId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public List<OrderDetailDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderDetailDTO> items) {
        this.items = items;
    }

    public PaymentDTO getPayment() {
        return payment;
    }

    public void setPayment(PaymentDTO payment) {
        this.payment = payment;
    }
}