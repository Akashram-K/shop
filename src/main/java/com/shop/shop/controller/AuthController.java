package com.shop.shop.controller;

import com.shop.shop.dto.ApiResponse;
import com.shop.shop.dto.AuthRequest;
import com.shop.shop.dto.RegisterRequest;
import com.shop.shop.dto.UserResponse;
import com.shop.shop.security.SecurityUtils;
import com.shop.shop.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UserResponse response = authService.register(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful! Welcome to RoyaL Jwellery.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UserResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Login successful!", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        if (!SecurityUtils.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.ok("Not authenticated", null));
        }
        UserResponse user = authService.getCurrentUserResponse();
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
