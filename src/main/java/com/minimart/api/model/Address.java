package com.minimart.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "address", schema = "final")
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    // REMOVED @NotNull - we'll validate in controller during CREATE
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long user_id;
    
    @NotNull(message = "Name is required")
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "home_no", length = 50)
    @JsonProperty("home_no")
    private String home_no;
    
    @Column(name = "street", length = 255)
    private String street;
    
    @Column(name = "district", length = 100)
    private String district;
    
    @Column(name = "province", length = 100)
    private String province;
    
    @NotNull(message = "Latitude is required")
    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @NotNull(message = "Longitude is required")
    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updated_at;
    
    // Constructors
    public Address() {
    }
    
    public Address(Long user_id, String name, String home_no, String street, String district, 
                   String province, BigDecimal latitude, BigDecimal longitude) {
        this.user_id = user_id;
        this.name = name;
        this.home_no = home_no;
        this.street = street;
        this.district = district;
        this.province = province;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Automatically set timestamps
    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now();
        updated_at = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updated_at = LocalDateTime.now();
    }
    
    // Getters and Setters (all remain the same)
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Long getUser_id() {
        return user_id;
    }
    
    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getHome_no() {
        return home_no;
    }
    
    public void setHome_no(String home_no) {
        this.home_no = home_no;
    }
    
    public String getStreet() {
        return street;
    }
    
    public void setStreet(String street) {
        this.street = street;
    }
    
    public String getDistrict() {
        return district;
    }
    
    public void setDistrict(String district) {
        this.district = district;
    }
    
    public String getProvince() {
        return province;
    }
    
    public void setProvince(String province) {
        this.province = province;
    }
    
    public BigDecimal getLatitude() {
        return latitude;
    }
    
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
    
    public BigDecimal getLongitude() {
        return longitude;
    }
    
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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
}