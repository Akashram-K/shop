package com.shop.shop;

import com.shop.shop.dto.CartItemRequest;
import com.shop.shop.dto.OrderRequest;
import com.shop.shop.dto.OrderResponse;
import com.shop.shop.entity.OrderStatus;
import com.shop.shop.entity.Product;
import com.shop.shop.entity.User;
import com.shop.shop.repository.NotificationRepository;
import com.shop.shop.repository.ProductRepository;
import com.shop.shop.repository.UserRepository;
import com.shop.shop.security.CustomUserDetails;
import com.shop.shop.service.CartService;
import com.shop.shop.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User demoCustomer;
    private Product demoProduct;

    @BeforeEach
    void setUp() {
        demoCustomer = userRepository.findByEmail("customer@gmail.com")
                .orElseThrow(() -> new IllegalStateException("Demo customer not found"));

        CustomUserDetails userDetails = new CustomUserDetails(demoCustomer);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        List<Product> products = productRepository.findByActiveTrueOrderByCreatedAtDesc();
        Assertions.assertFalse(products.isEmpty());
        demoProduct = products.get(0);
    }

    @Test
    void testOrderPlacementAndStockReductionAndNotification() {
        int initialStock = demoProduct.getStockQuantity();
        long initialNotifCount = notificationRepository.count();

        // 1. Add item to cart
        cartService.addItemToCart(new CartItemRequest(demoProduct.getId(), 2));

        // 2. Place order
        OrderRequest orderReq = new OrderRequest();
        orderReq.setRecipientName("Rahul Sharma");
        orderReq.setPhone("+91 98123 45678");
        orderReq.setStreet("42 Emerald Heights");
        orderReq.setCity("Bengaluru");
        orderReq.setState("Karnataka");
        orderReq.setPincode("560001");
        orderReq.setPaymentMethod("Cash on Delivery");

        OrderResponse orderResponse = orderService.placeOrder(orderReq);

        // 3. Verify Order Properties
        Assertions.assertNotNull(orderResponse);
        Assertions.assertEquals("PLACED", orderResponse.getStatus());
        Assertions.assertEquals(2, orderResponse.getTotalItems());

        // 4. Verify Stock was reduced by 2
        Product updatedProduct = productRepository.findById(demoProduct.getId()).orElse(null);
        Assertions.assertNotNull(updatedProduct);
        Assertions.assertEquals(initialStock - 2, updatedProduct.getStockQuantity());

        // 5. Verify Notification was generated for admin
        Assertions.assertEquals(initialNotifCount + 1, notificationRepository.count());

        // 6. Test Admin Updating Status to CONFIRMED and DELIVERED
        OrderResponse confirmed = orderService.updateOrderStatus(orderResponse.getId(), OrderStatus.CONFIRMED);
        Assertions.assertEquals("CONFIRMED", confirmed.getStatus());

        OrderResponse delivered = orderService.updateOrderStatus(orderResponse.getId(), OrderStatus.DELIVERED);
        Assertions.assertEquals("DELIVERED", delivered.getStatus());
    }
}
