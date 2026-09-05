package com.shop.shop.service;

import com.shop.shop.dto.CategoryDTO;
import com.shop.shop.dto.ProductDTO;
import com.shop.shop.entity.Category;
import com.shop.shop.entity.Product;
import com.shop.shop.repository.CategoryRepository;
import com.shop.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getCustomerProducts(String query, Long categoryId, Boolean featuredOnly) {
        List<Product> products;

        if (query != null && !query.trim().isEmpty()) {
            String trimmed = query.trim();
            if (categoryId != null && categoryId > 0) {
                products = productRepository.searchActiveProductsByCategory(categoryId, trimmed);
            } else {
                products = productRepository.searchActiveProducts(trimmed);
            }
        } else if (categoryId != null && categoryId > 0) {
            products = productRepository.findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(categoryId);
        } else if (Boolean.TRUE.equals(featuredOnly)) {
            products = productRepository.findByActiveTrueAndIsFeaturedTrueOrderByCreatedAtDesc();
        } else {
            products = productRepository.findByActiveTrueOrderByCreatedAtDesc();
        }

        return products.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        return mapToDTO(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProductsForAdmin() {
        return productRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto) {
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + dto.getCategoryId()));
        }

        Product product = new Product(
                dto.getName().trim(),
                dto.getDescription() != null ? dto.getDescription().trim() : "",
                dto.getPrice(),
                dto.getImageUrl() != null ? dto.getImageUrl().trim() : "",
                category,
                dto.getStockQuantity() != null ? dto.getStockQuantity() : 0,
                dto.getActive() != null ? dto.getActive() : true,
                dto.getIsFeatured() != null ? dto.getIsFeatured() : false,
                dto.getKaratOrPurity() != null ? dto.getKaratOrPurity().trim() : "",
                dto.getWeightGrams() != null ? dto.getWeightGrams().trim() : ""
        );

        product = productRepository.save(product);
        return mapToDTO(product);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            product.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription().trim());
        }
        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }
        if (dto.getImageUrl() != null) {
            product.setImageUrl(dto.getImageUrl().trim());
        }
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }
        if (dto.getStockQuantity() != null) {
            product.setStockQuantity(dto.getStockQuantity());
        }
        if (dto.getActive() != null) {
            product.setActive(dto.getActive());
        }
        if (dto.getIsFeatured() != null) {
            product.setIsFeatured(dto.getIsFeatured());
        }
        if (dto.getKaratOrPurity() != null) {
            product.setKaratOrPurity(dto.getKaratOrPurity().trim());
        }
        if (dto.getWeightGrams() != null) {
            product.setWeightGrams(dto.getWeightGrams().trim());
        }

        product = productRepository.save(product);
        return mapToDTO(product);
    }

    @Transactional
    public ProductDTO toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        product.setActive(!product.getActive());
        product = productRepository.save(product);
        return mapToDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        // Soft delete / deactivation or full removal
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getIcon(),
                        c.getImageUrl(),
                        c.getProducts() != null ? c.getProducts().stream().filter(Product::getActive).count() : 0
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (categoryRepository.existsByName(dto.getName().trim())) {
            throw new IllegalArgumentException("Category already exists: " + dto.getName());
        }
        Category category = new Category(
                dto.getName().trim(),
                dto.getDescription() != null ? dto.getDescription().trim() : "",
                dto.getIcon() != null ? dto.getIcon().trim() : "",
                dto.getImageUrl() != null ? dto.getImageUrl().trim() : ""
        );
        category = categoryRepository.save(category);
        return new CategoryDTO(category.getId(), category.getName(), category.getDescription(), category.getIcon(), category.getImageUrl(), 0);
    }

    public ProductDTO mapToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        dto.setStockQuantity(product.getStockQuantity());
        dto.setActive(product.getActive());
        dto.setIsFeatured(product.getIsFeatured());
        dto.setKaratOrPurity(product.getKaratOrPurity());
        dto.setWeightGrams(product.getWeightGrams());
        dto.setCreatedAt(product.getCreatedAt());
        return dto;
    }
}
