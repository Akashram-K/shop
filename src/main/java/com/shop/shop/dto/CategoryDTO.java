package com.shop.shop.dto;

public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String imageUrl;
    private long productCount;

    public CategoryDTO() {
    }

    public CategoryDTO(Long id, String name, String description, String icon, String imageUrl, long productCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.imageUrl = imageUrl;
        this.productCount = productCount;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getProductCount() {
        return productCount;
    }

    public void setProductCount(long productCount) {
        this.productCount = productCount;
    }
}
