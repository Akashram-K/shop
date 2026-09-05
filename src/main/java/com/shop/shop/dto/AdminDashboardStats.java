package com.shop.shop.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardStats {
    private long totalProducts;
    private long totalCustomers;
    private long totalOrders;
    private long pendingOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private BigDecimal totalSales = BigDecimal.ZERO;
    private long unreadNotificationsCount;
    private List<OrderResponse> recentOrders = new ArrayList<>();
    private List<ProductDTO> lowStockProducts = new ArrayList<>();

    public AdminDashboardStats() {
    }

    // Getters and Setters
    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getDeliveredOrders() {
        return deliveredOrders;
    }

    public void setDeliveredOrders(long deliveredOrders) {
        this.deliveredOrders = deliveredOrders;
    }

    public long getCancelledOrders() {
        return cancelledOrders;
    }

    public void setCancelledOrders(long cancelledOrders) {
        this.cancelledOrders = cancelledOrders;
    }

    public BigDecimal getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }

    public long getUnreadNotificationsCount() {
        return unreadNotificationsCount;
    }

    public void setUnreadNotificationsCount(long unreadNotificationsCount) {
        this.unreadNotificationsCount = unreadNotificationsCount;
    }

    public List<OrderResponse> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<OrderResponse> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<ProductDTO> getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(List<ProductDTO> lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }
}
