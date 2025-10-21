package com.minimart.api.dto;

import java.math.BigDecimal;

public class OrderDetailDTO {
    private Long id;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer qty;
    private BigDecimal price;
    private BigDecimal subtotal;
    
    // Constructors
    public OrderDetailDTO() {}
    
    public OrderDetailDTO(Long id, Integer productId, String productName, 
                         String productImage, Integer qty, BigDecimal price) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
        this.qty = qty;
        this.price = price;
        this.subtotal = price.multiply(new BigDecimal(qty));
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getProductId() {
        return productId;
    }
    
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getProductImage() {
        return productImage;
    }
    
    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }
    
    public Integer getQty() {
        return qty;
    }
    
    public void setQty(Integer qty) {
        this.qty = qty;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}