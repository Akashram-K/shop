package com.shop.shop.service;

import com.shop.shop.dto.AddressDTO;
import com.shop.shop.dto.AuthRequest;
import com.shop.shop.dto.RegisterRequest;
import com.shop.shop.dto.UserResponse;
import com.shop.shop.entity.Address;
import com.shop.shop.entity.Cart;
import com.shop.shop.entity.Role;
import com.shop.shop.entity.User;
import com.shop.shop.repository.AddressRepository;
import com.shop.shop.repository.CartRepository;
import com.shop.shop.repository.UserRepository;
import com.shop.shop.security.CustomUserDetails;
import com.shop.shop.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       AddressRepository addressRepository,
                       CartRepository cartRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_CUSTOMER);

        user = userRepository.save(user);

        // Create default address
        Address address = new Address(
                user,
                user.getFullName(),
                user.getPhone(),
                request.getStreet().trim(),
                request.getCity().trim(),
                request.getState().trim(),
                request.getPincode().trim(),
                request.getLandmark() != null ? request.getLandmark().trim() : null,
                true
        );
        addressRepository.save(address);

        // Create user cart
        Cart cart = new Cart(user);
        cartRepository.save(cart);

        // Automatically log in newly registered user
        authenticateAndSetSession(email, request.getPassword(), httpRequest, httpResponse);

        return mapToUserResponse(user);
    }

    public UserResponse login(AuthRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.getEmail().toLowerCase().trim();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            return mapToUserResponse(user);
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password. Please check your credentials.");
        }
    }

    private void authenticateAndSetSession(String email, String rawPassword, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword)
        );
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserResponse() {
        User user = SecurityUtils.getCurrentUser()
                .flatMap(u -> userRepository.findById(u.getId()))
                .orElseThrow(() -> new IllegalStateException("No user is currently authenticated"));
        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        return SecurityUtils.getCurrentUser()
                .flatMap(u -> userRepository.findById(u.getId()))
                .orElseThrow(() -> new IllegalStateException("User authentication required"));
    }

    @Transactional
    public UserResponse updateProfile(String fullName, String phone) {
        User user = getCurrentAuthenticatedUser();
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null && !phone.isBlank()) {
            user.setPhone(phone.trim());
        }
        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public AddressDTO addAddress(AddressDTO dto) {
        User user = getCurrentAuthenticatedUser();

        if (dto.isDefault()) {
            List<Address> existing = addressRepository.findByUserId(user.getId());
            for (Address a : existing) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        }

        Address address = new Address(
                user,
                dto.getRecipientName() != null ? dto.getRecipientName() : user.getFullName(),
                dto.getPhone() != null ? dto.getPhone() : user.getPhone(),
                dto.getStreet(),
                dto.getCity(),
                dto.getState(),
                dto.getPincode(),
                dto.getLandmark(),
                dto.isDefault()
        );

        address = addressRepository.save(address);
        return mapToAddressDTO(address);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses() {
        User user = getCurrentAuthenticatedUser();
        return addressRepository.findByUserId(user.getId()).stream()
                .map(this::mapToAddressDTO)
                .collect(Collectors.toList());
    }

    public UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name()
        );

        List<Address> addresses = addressRepository.findByUserId(user.getId());
        response.setAddresses(addresses.stream().map(this::mapToAddressDTO).collect(Collectors.toList()));
        return response;
    }

    public AddressDTO mapToAddressDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setRecipientName(address.getRecipientName());
        dto.setPhone(address.getPhone());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPincode(address.getPincode());
        dto.setLandmark(address.getLandmark());
        dto.setDefault(address.isDefault());
        dto.setFormattedAddress(address.getFormattedAddress());
        return dto;
    }
}
