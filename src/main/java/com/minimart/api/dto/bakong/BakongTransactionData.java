package com.minimart.api.dto.bakong;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class BakongTransactionData {
    
    @JsonProperty("hash")
    private String hash;
    
    @JsonProperty("fromAccountId")
    private String fromAccountId;
    
    @JsonProperty("toAccountId")
    private String toAccountId;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("description")
    private String description;
    
    public BakongTransactionData() {}
    
    // Getters and Setters
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getFromAccountId() {
        return fromAccountId;
    }
    
    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }
    
    public String getToAccountId() {
        return toAccountId;
    }
    
    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}