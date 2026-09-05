package com.shop.shop.service;

import com.shop.shop.dto.CartItemDTO;
import com.shop.shop.dto.CartItemRequest;
import com.shop.shop.dto.CartResponse;
import com.shop.shop.entity.Cart;
import com.shop.shop.entity.CartItem;
import com.shop.shop.entity.Product;
import com.shop.shop.entity.User;
import com.shop.shop.repository.CartItemRepository;
import com.shop.shop.repository.CartRepository;
import com.shop.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       AuthService authService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.authService = authService;
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart(user);
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartResponse getCartForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        return mapToCartResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(CartItemRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (!product.getActive()) {
            throw new IllegalArgumentException("This product is currently unavailable.");
        }

        if (product.getStockQuantity() <= 0) {
            throw new IllegalArgumentException("Sorry, '" + product.getName() + "' is out of stock.");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        int requestedQty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            int newQty = item.getQuantity() + requestedQty;
            if (newQty > product.getStockQuantity()) {
                throw new IllegalArgumentException("Cannot add more. Available stock is " + product.getStockQuantity() + " items.");
            }
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            if (requestedQty > product.getStockQuantity()) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock of " + product.getStockQuantity() + ".");
            }
            CartItem newItem = new CartItem(cart, product, requestedQty);
            cartItemRepository.save(newItem);
        }

        return getCartForCurrentUser();
    }

    @Transactional
    public CartResponse updateItemQuantity(Long itemId, int quantity) {
        User user = authService.getCurrentAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Unauthorized modification of cart item");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (quantity > product.getStockQuantity()) {
                throw new IllegalArgumentException("Only " + product.getStockQuantity() + " items are available in stock.");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCartForCurrentUser();
    }

    @Transactional
    public CartResponse removeItem(Long itemId) {
        User user = authService.getCurrentAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Unauthorized modification of cart item");
        }

        cartItemRepository.delete(item);
        return getCartForCurrentUser();
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    public CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemDTO> itemDTOs = new ArrayList<>();

        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : items) {
            Product p = item.getProduct();
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalItems += item.getQuantity();
            totalAmount = totalAmount.add(subtotal);

            itemDTOs.add(new CartItemDTO(
                    item.getId(),
                    p.getId(),
                    p.getName(),
                    p.getPrice(),
                    p.getImageUrl(),
                    p.getKaratOrPurity(),
                    item.getQuantity(),
                    p.getStockQuantity(),
                    subtotal
            ));
        }

        return new CartResponse(cart.getId(), itemDTOs, totalItems, totalAmount);
    }
}
