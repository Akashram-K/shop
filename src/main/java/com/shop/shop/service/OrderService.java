package com.shop.shop.service;

import com.shop.shop.dto.*;
import com.shop.shop.entity.*;
import com.shop.shop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductRepository productRepository,
                        AddressRepository addressRepository,
                        UserRepository userRepository,
                        AuthService authService,
                        NotificationService notificationService,
                        ProductService productService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.notificationService = notificationService;
        this.productService = productService;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot place order with an empty cart.");
        }

        // Validate stock for all items
        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            if (!product.getActive()) {
                throw new IllegalArgumentException("Product '" + product.getName() + "' is no longer available.");
            }
            if (item.getQuantity() > product.getStockQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for '" + product.getName() + "'. Available: " + product.getStockQuantity());
            }
        }

        // Determine delivery address and recipient details
        String recipientName = user.getFullName();
        String recipientPhone = user.getPhone();
        String formattedAddress;

        if (request.getAddressId() != null) {
            Address address = addressRepository.findByIdAndUserId(request.getAddressId(), user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected address not found"));
            recipientName = address.getRecipientName() != null ? address.getRecipientName() : user.getFullName();
            recipientPhone = address.getPhone() != null ? address.getPhone() : user.getPhone();
            formattedAddress = address.getFormattedAddress();
        } else if (request.getStreet() != null && !request.getStreet().isBlank()) {
            if (request.getRecipientName() != null && !request.getRecipientName().isBlank()) {
                recipientName = request.getRecipientName().trim();
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                recipientPhone = request.getPhone().trim();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(recipientName).append(", Ph: ").append(recipientPhone).append(", ");
            sb.append(request.getStreet().trim()).append(", ");
            if (request.getLandmark() != null && !request.getLandmark().isBlank()) {
                sb.append("Near ").append(request.getLandmark().trim()).append(", ");
            }
            sb.append(request.getCity().trim()).append(", ").append(request.getState().trim()).append(" - ").append(request.getPincode().trim());
            formattedAddress = sb.toString();

            if (request.isSaveAddress()) {
                Address newAddress = new Address(
                        user,
                        recipientName,
                        recipientPhone,
                        request.getStreet().trim(),
                        request.getCity().trim(),
                        request.getState().trim(),
                        request.getPincode().trim(),
                        request.getLandmark() != null ? request.getLandmark().trim() : null,
                        false
                );
                addressRepository.save(newAddress);
            }
        } else {
            // Check if user has any default address
            Address defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .orElseGet(() -> {
                        List<Address> addresses = addressRepository.findByUserId(user.getId());
                        if (addresses.isEmpty()) {
                            throw new IllegalArgumentException("Please provide a delivery address.");
                        }
                        return addresses.get(0);
                    });
            recipientName = defaultAddress.getRecipientName() != null ? defaultAddress.getRecipientName() : user.getFullName();
            recipientPhone = defaultAddress.getPhone() != null ? defaultAddress.getPhone() : user.getPhone();
            formattedAddress = defaultAddress.getFormattedAddress();
        }

        // Generate Order Number
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int randomCode = 1000 + new Random().nextInt(9000);
        String orderNumber = "RJ-" + datePrefix + "-" + randomCode;

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        Order order = new Order(
                orderNumber,
                user,
                BigDecimal.ZERO,
                OrderStatus.PLACED,
                recipientName,
                recipientPhone,
                formattedAddress,
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "Cash on Delivery",
                request.getNotes()
        );

        order = orderRepository.save(order);

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    product.getName(),
                    product.getPrice(),
                    item.getQuantity(),
                    subtotal,
                    product.getImageUrl()
            );
            orderItems.add(orderItem);

            // Decrement product stock
            product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        order = orderRepository.save(order);

        // Clear Cart
        cartItemRepository.deleteByCartId(cart.getId());

        // In-app Notification for Admin / Father
        notificationService.createOrderNotification(order);

        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders() {
        User user = authService.getCurrentAuthenticatedUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        // If customer, verify ownership
        if (user.getRole() != Role.ROLE_ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized to view this order");
        }

        return mapToOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        if (user.getRole() != Role.ROLE_ADMIN && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized to cancel this order");
        }

        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Restore stock
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }

        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrdersForAdmin(String query, OrderStatus status) {
        List<Order> orders;

        if (query != null && !query.trim().isEmpty()) {
            orders = orderRepository.searchOrders(query.trim());
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + id));

        // If transitioning to CANCELLED from non-cancelled, restore stock
        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminDashboardStats() {
        AdminDashboardStats stats = new AdminDashboardStats();

        stats.setTotalProducts(productRepository.count());
        stats.setTotalCustomers(userRepository.countByRole(Role.ROLE_CUSTOMER));
        stats.setTotalOrders(orderRepository.count());

        long placed = orderRepository.countByStatus(OrderStatus.PLACED);
        long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long packed = orderRepository.countByStatus(OrderStatus.PACKED);
        long outForDelivery = orderRepository.countByStatus(OrderStatus.OUT_FOR_DELIVERY);

        stats.setPendingOrders(placed + confirmed + packed + outForDelivery);
        stats.setDeliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.setCancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED));

        BigDecimal totalSales = orderRepository.calculateTotalSales();
        stats.setTotalSales(totalSales != null ? totalSales : BigDecimal.ZERO);

        stats.setUnreadNotificationsCount(notificationService.getUnreadCount());

        List<Order> recentOrders = orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(10)
                .toList();
        stats.setRecentOrders(recentOrders.stream().map(this::mapToOrderResponse).collect(Collectors.toList()));

        List<Product> lowStock = productRepository.findByStockQuantityLessThanEqualAndActiveTrue(3);
        stats.setLowStockProducts(lowStock.stream().map(productService::mapToDTO).collect(Collectors.toList()));

        return stats;
    }

    public OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setCustomerId(order.getUser().getId());
        response.setCustomerName(order.getUser().getFullName());
        response.setCustomerEmail(order.getUser().getEmail());
        response.setCustomerPhone(order.getUser().getPhone());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setRecipientName(order.getRecipientName());
        response.setRecipientPhone(order.getRecipientPhone());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setNotes(order.getNotes());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemDTO> itemDTOs = order.getItems().stream().map(item -> new OrderItemDTO(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductName(),
                item.getProductPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getImageUrl()
        )).collect(Collectors.toList());

        response.setItems(itemDTOs);
        response.setTotalItems(itemDTOs.stream().mapToInt(OrderItemDTO::getQuantity).sum());
        return response;
    }
}
