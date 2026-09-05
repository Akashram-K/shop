package com.shop.shop.controller;

import com.shop.shop.dto.ApiResponse;
import com.shop.shop.dto.CategoryDTO;
import com.shop.shop.dto.ProductDTO;
import com.shop.shop.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean featured) {
        List<ProductDTO> products = productService.getCustomerProducts(search, categoryId, featured);
        return ResponseEntity.ok(ApiResponse.ok("Products loaded", products));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok("Product details loaded", product));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getCategories() {
        List<CategoryDTO> categories = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok("Categories loaded", categories));
    }
}
