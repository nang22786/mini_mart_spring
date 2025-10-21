package com.minimart.api.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orderdetail", schema = "final")
public class OrderDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id", nullable = false)
    private Integer productId;
    
    @Column(name = "qty", nullable = false)
    private Integer qty;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    // Constructors
    public OrderDetail() {}
    
    public OrderDetail(Integer productId, Integer qty, BigDecimal price) {
        this.productId = productId;
        this.qty = qty;
        this.price = price;
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
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
}