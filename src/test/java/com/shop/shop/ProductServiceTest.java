package com.shop.shop;

import com.shop.shop.dto.ProductDTO;
import com.shop.shop.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Transactional
public class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void testProductSearchAndFiltering() {
        List<ProductDTO> all = productService.getCustomerProducts(null, null, null);
        Assertions.assertFalse(all.isEmpty());

        List<ProductDTO> searchNecklace = productService.getCustomerProducts("necklace", null, null);
        Assertions.assertFalse(searchNecklace.isEmpty());
        for (ProductDTO p : searchNecklace) {
            boolean matches = p.getName().toLowerCase().contains("necklace") ||
                    p.getDescription().toLowerCase().contains("necklace") ||
                    (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains("necklace"));
            Assertions.assertTrue(matches);
        }
    }

    @Test
    void testAdminProductCreationAndUpdate() {
        ProductDTO newProduct = new ProductDTO();
        newProduct.setName("Royal Diamond Tiara");
        newProduct.setDescription("A magnificent handcrafted royal diamond tiara.");
        newProduct.setPrice(new BigDecimal("350000.00"));
        newProduct.setStockQuantity(3);
        newProduct.setKaratOrPurity("18K White Gold & VVS1 Diamonds");
        newProduct.setWeightGrams("65.00 g");
        newProduct.setImageUrl("https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f");
        newProduct.setActive(true);

        ProductDTO created = productService.createProduct(newProduct);
        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals("Royal Diamond Tiara", created.getName());

        // Update price and stock
        created.setPrice(new BigDecimal("340000.00"));
        created.setStockQuantity(5);
        ProductDTO updated = productService.updateProduct(created.getId(), created);

        Assertions.assertEquals(new BigDecimal("340000.00"), updated.getPrice());
        Assertions.assertEquals(5, updated.getStockQuantity());
    }
}
