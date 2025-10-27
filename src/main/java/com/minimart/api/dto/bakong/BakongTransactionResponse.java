package com.minimart.api.dto.bakong;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class BakongTransactionResponse {
    
    @JsonProperty("responseCode")
    private int responseCode;
    
    @JsonProperty("responseMessage")
    private String responseMessage;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("data")
    private BakongTransactionData data;

    // Check if payment is successful
    public boolean isPaid() {
        return responseCode == 0 && data != null && data.getHash() != null;
    }

    // Getters and setters
    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public BakongTransactionData getData() {
        return data;
    }

    public void setData(BakongTransactionData data) {
        this.data = data;
    }

    public static class BakongTransactionData {
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
        
        @JsonProperty("createdDateTime")
        private Long createdDateTime;
        
        @JsonProperty("acknowledgedDateTime")
        private Long acknowledgedDateTime;

        // Getters and setters
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

        public Long getCreatedDateTime() {
            return createdDateTime;
        }

        public void setCreatedDateTime(Long createdDateTime) {
            this.createdDateTime = createdDateTime;
        }

        public Long getAcknowledgedDateTime() {
            return acknowledgedDateTime;
        }

        public void setAcknowledgedDateTime(Long acknowledgedDateTime) {
            this.acknowledgedDateTime = acknowledgedDateTime;
        }
    }
}