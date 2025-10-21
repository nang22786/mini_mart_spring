package com.minimart.api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertising", schema = "final")
public class Advertising {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "image", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Constructors
    public Advertising() {
    }

    public Advertising(Integer id, String imageUrl, LocalDateTime createDate) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.createDate = createDate;
    }

    // Getters
    public Integer getId() {
        return id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    // Setters
    public void setId(Integer id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }
    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @PrePersist
    protected void onCreate() {
        if (createDate == null) {
            createDate = LocalDateTime.now();
        }
    }
}