package com.shop.shop.controller;

import com.shop.shop.dto.ApiResponse;
import com.shop.shop.dto.CartItemRequest;
import com.shop.shop.dto.CartResponse;
import com.shop.shop.service.AuthService;
import com.shop.shop.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final AuthService authService;

    public CartController(CartService cartService, AuthService authService) {
        this.cartService = cartService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse cart = cartService.getCartForCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Cart loaded", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.addItemToCart(request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        Integer quantity = payload.get("quantity");
        if (quantity == null) {
            quantity = 1;
        }
        CartResponse cart = cartService.updateItemQuantity(id, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Cart updated", cart));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long id) {
        CartResponse cart = cartService.removeItem(id);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart(authService.getCurrentAuthenticatedUser());
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared"));
    }
}
