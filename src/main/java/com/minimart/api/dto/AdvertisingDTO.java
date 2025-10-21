package com.minimart.api.dto;

import java.time.LocalDateTime;

public class AdvertisingDTO {
    private Integer id;
    private String imageUrl;
    private LocalDateTime createDate;
    private Boolean isActive;

    // Constructors
    public AdvertisingDTO() {
    }

    public AdvertisingDTO(Integer id, String imageUrl, LocalDateTime createDate) {
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

}