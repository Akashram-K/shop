package com.shop.shop;

import com.shop.shop.dto.AuthRequest;
import com.shop.shop.dto.RegisterRequest;
import com.shop.shop.dto.UserResponse;
import com.shop.shop.entity.Role;
import com.shop.shop.entity.User;
import com.shop.shop.repository.UserRepository;
import com.shop.shop.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testUserRegistrationAndBCryptPasswordHashing() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Priya Patel");
        request.setEmail("priya.test@example.com");
        request.setPhone("+91 99887 76655");
        request.setPassword("priyaPass123");
        request.setStreet("12 Lotus Temple Rd");
        request.setCity("Ahmedabad");
        request.setState("Gujarat");
        request.setPincode("380001");

        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        MockHttpServletResponse mockResp = new MockHttpServletResponse();

        UserResponse userResponse = authService.register(request, mockReq, mockResp);

        Assertions.assertNotNull(userResponse);
        Assertions.assertEquals("Priya Patel", userResponse.getFullName());
        Assertions.assertEquals("priya.test@example.com", userResponse.getEmail());
        Assertions.assertEquals(Role.ROLE_CUSTOMER.name(), userResponse.getRole());

        // Verify password is hashed with BCrypt and not plain text
        User savedUser = userRepository.findByEmail("priya.test@example.com").orElse(null);
        Assertions.assertNotNull(savedUser);
        Assertions.assertNotEquals("priyaPass123", savedUser.getPassword());
        Assertions.assertTrue(passwordEncoder.matches("priyaPass123", savedUser.getPassword()));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest loginReq = new AuthRequest("customer@gmail.com", "customer123");
        MockHttpServletRequest mockReq = new MockHttpServletRequest();
        MockHttpServletResponse mockResp = new MockHttpServletResponse();

        UserResponse response = authService.login(loginReq, mockReq, mockResp);
        Assertions.assertNotNull(response);
        Assertions.assertEquals("customer@gmail.com", response.getEmail());
    }
}
