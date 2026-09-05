package com.shop.shop;

import com.shop.shop.service.FileStorageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
public class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void testStoreValidImageFile() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "ruby_necklace.jpg",
                "image/jpeg",
                "fake image content for testing".getBytes()
        );

        String storedPath = fileStorageService.storeFile(mockFile);
        Assertions.assertNotNull(storedPath);
        Assertions.assertTrue(storedPath.startsWith("/uploads/products/"));
        Assertions.assertTrue(storedPath.endsWith(".jpg"));
    }

    @Test
    void testStoreInvalidFileTypeThrowsException() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "malicious_script.exe",
                "application/octet-stream",
                "evil content".getBytes()
        );

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.storeFile(invalidFile);
        });
    }
}
