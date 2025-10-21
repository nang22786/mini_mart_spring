package com.minimart.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PendingOrderDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private Integer itemCount;
    
    // Constructors
    public PendingOrderDTO() {}
    
    public PendingOrderDTO(Long id, Long userId, String userName, String userEmail,
                          BigDecimal amount, String paymentMethod, 
                          LocalDateTime createdAt, Integer itemCount) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.itemCount = itemCount;
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
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
}