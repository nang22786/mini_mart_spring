package com.minimart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private Integer addressId;
    private String addressType;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;
}