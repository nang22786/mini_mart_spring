package com.minimart.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart", schema = "final")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
    private User user;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer user_id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;
    
    @Column(name = "product_id", insertable = false, updatable = false)
    private Integer product_id;
    
    @Column(nullable = false)
    private Integer qty;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime created_at;
    
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    
    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now();
        updated_at = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updated_at = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Integer getUser_id() {
        return user_id;
    }
    
    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Integer getProduct_id() {
        return product_id;
    }
    
    public void setProduct_id(Integer product_id) {
        this.product_id = product_id;
    }
    
    public Integer getQty() {
        return qty;
    }
    
    public void setQty(Integer qty) {
        this.qty = qty;
    }
    
    public LocalDateTime getCreated_at() {
        return created_at;
    }
    
    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }
    
    public LocalDateTime getUpdated_at() {
        return updated_at;
    }
    
    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }
    
    // Helper method to calculate subtotal
    @Transient
    public BigDecimal getSubtotal() {
        if (product != null && product.getPrice() != null && qty != null) {
            return product.getPrice().multiply(BigDecimal.valueOf(qty));
        }
        return BigDecimal.ZERO;
    }
}