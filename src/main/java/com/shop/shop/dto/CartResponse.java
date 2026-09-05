package com.shop.shop.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartResponse {
    private Long id;
    private List<CartItemDTO> items = new ArrayList<>();
    private int totalItems = 0;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public CartResponse() {
    }

    public CartResponse(Long id, List<CartItemDTO> items, int totalItems, BigDecimal totalAmount) {
        this.id = id;
        this.items = items;
        this.totalItems = totalItems;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
