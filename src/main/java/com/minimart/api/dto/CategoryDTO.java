package com.minimart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Integer categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
}