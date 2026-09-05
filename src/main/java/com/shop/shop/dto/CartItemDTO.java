package com.shop.shop.dto;

import java.math.BigDecimal;

public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String imageUrl;
    private String karatOrPurity;
    private Integer quantity;
    private Integer availableStock;
    private BigDecimal subtotal;

    public CartItemDTO() {
    }

    public CartItemDTO(Long id, Long productId, String productName, BigDecimal productPrice, String imageUrl, String karatOrPurity, Integer quantity, Integer availableStock, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.imageUrl = imageUrl;
        this.karatOrPurity = karatOrPurity;
        this.quantity = quantity;
        this.availableStock = availableStock;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductPrice() {
        return productPrice;
    }

    public void setProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getKaratOrPurity() {
        return karatOrPurity;
    }

    public void setKaratOrPurity(String karatOrPurity) {
        this.karatOrPurity = karatOrPurity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
