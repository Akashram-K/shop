package com.shop.shop.controller;

import com.shop.shop.dto.*;
import com.shop.shop.entity.OrderStatus;
import com.shop.shop.entity.Role;
import com.shop.shop.repository.UserRepository;
import com.shop.shop.service.AuthService;
import com.shop.shop.service.NotificationService;
import com.shop.shop.service.OrderService;
import com.shop.shop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.shop.shop.service.*;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final FileStorageService fileStorageService;

    public AdminController(OrderService orderService,
                           ProductService productService,
                           NotificationService notificationService,
                           UserRepository userRepository,
                           AuthService authService,
                           FileStorageService fileStorageService) {
        this.orderService = orderService;
        this.productService = productService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getStats() {
        AdminDashboardStats stats = orderService.getAdminDashboardStats();
        return ResponseEntity.ok(ApiResponse.ok("Admin stats loaded", stats));
    }

    // Products Management
    @PostMapping(value = "/products/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(ApiResponse.ok("Image uploaded successfully", Map.of("imageUrl", imageUrl)));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProductsForAdmin();
        return ResponseEntity.ok(ApiResponse.ok("Products loaded", products));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        ProductDTO created = productService.createProduct(productDTO);
        return ResponseEntity.ok(ApiResponse.ok("Product added successfully", created));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updated = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }

    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<ProductDTO>> toggleProductStatus(@PathVariable Long id) {
        ProductDTO updated = productService.toggleProductStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Product status updated", updated));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted successfully"));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO created = productService.createCategory(categoryDTO);
        return ResponseEntity.ok(ApiResponse.ok("Category created successfully", created));
    }

    // Orders Management
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status) {
        List<OrderResponse> orders = orderService.getAllOrdersForAdmin(search, status);
        return ResponseEntity.ok(ApiResponse.ok("Orders loaded", orders));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse updated = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated to " + request.getStatus(), updated));
    }

    // Customers Directory
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getCustomers() {
        List<UserResponse> customers = userRepository.findByRole(Role.ROLE_CUSTOMER).stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Customers list loaded", customers));
    }

    // Notifications
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications() {
        List<NotificationDTO> list = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponse.ok("Notifications loaded", list));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }
}
