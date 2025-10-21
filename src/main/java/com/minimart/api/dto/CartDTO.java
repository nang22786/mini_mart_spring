package com.minimart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Integer cartId;
    private Integer productId;
    private String productName;
    private BigDecimal price;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal subtotal;
}