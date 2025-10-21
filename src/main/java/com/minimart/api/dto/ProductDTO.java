package com.minimart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Integer productId;
    private Integer categoryId;
    private String categoryName;
    private String productName;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private String status;
    private Integer stockQuantity;
}