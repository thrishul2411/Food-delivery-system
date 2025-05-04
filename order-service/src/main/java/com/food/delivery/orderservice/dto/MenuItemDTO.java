package com.food.delivery.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data // Lombok
public class MenuItemDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    // Add any other fields needed for validation/order creation


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}