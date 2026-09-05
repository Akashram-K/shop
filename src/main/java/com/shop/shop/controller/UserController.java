package com.shop.shop.controller;

import com.shop.shop.dto.AddressDTO;
import com.shop.shop.dto.ApiResponse;
import com.shop.shop.dto.UserResponse;
import com.shop.shop.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        UserResponse user = authService.getCurrentUserResponse();
        return ResponseEntity.ok(ApiResponse.ok("Profile loaded", user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String phone = payload.get("phone");
        UserResponse updated = authService.updateProfile(fullName, phone);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressDTO>>> getAddresses() {
        List<AddressDTO> addresses = authService.getUserAddresses();
        return ResponseEntity.ok(ApiResponse.ok("Addresses loaded", addresses));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressDTO>> addAddress(@RequestBody AddressDTO dto) {
        AddressDTO created = authService.addAddress(dto);
        return ResponseEntity.ok(ApiResponse.ok("Address saved successfully", created));
    }
}
