package com.minimart.api.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "stock", schema = "final")
public class Stock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "qty", nullable = false)
    private Integer qty;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    @JsonIgnore
    private Product product;
    
    // Constructors
    public Stock() {}
    
    public Stock(Integer qty) {
        this.qty = qty;
    }
    
    public Stock(Integer qty, Product product) {
        this.qty = qty;
        this.product = product;
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getQty() {
        return qty;
    }
    
    public void setQty(Integer qty) {
        this.qty = qty;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    @Override
    public String toString() {
        return "Stock{" +
                "id=" + id +
                ", qty=" + qty +
                ", productId=" + (product != null ? product.getId() : null) +
                '}';
    }
}