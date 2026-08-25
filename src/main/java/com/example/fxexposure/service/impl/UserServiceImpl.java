package com.example.fxexposure.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.AuthResponse;
import com.example.fxexposure.dto.LoginRequest;
import com.example.fxexposure.dto.RegisterRequest;
import com.example.fxexposure.dto.UserDto;
import com.example.fxexposure.entity.User;
import com.example.fxexposure.enums.Role;
import com.example.fxexposure.exception.BusinessValidationException;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.UserRepository;
import com.example.fxexposure.security.JwtTokenProvider;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email is already registered: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ANALYST;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log("USER_REGISTERED", "User", saved.getId(), "User registered with role " + role);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword()));
        String token = tokenProvider.generateToken(auth);

        return AuthResponse.builder()
                .token(token)
                .user(mapToDto(saved))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword()));

        String token = tokenProvider.generateToken(auth);
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return AuthResponse.builder()
                .token(token)
                .user(mapToDto(user))
                .build();
    }

    @Override
    public UserDto getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found for: " + email));
        return mapToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto updateUserRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRole(role);
        User saved = userRepository.save(user);
        auditService.log("UPDATE_USER_ROLE", "User", id, "Updated user role to " + role);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public UserDto toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        auditService.log("TOGGLE_USER_STATUS", "User", id, "Set enabled=" + user.isEnabled());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void seedDefaultUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        createUserIfAbsent("Admin User", "admin@example.com", "Admin@123", Role.ADMIN);
        createUserIfAbsent("Treasury Manager", "manager@example.com", "Manager@123", Role.MANAGER);
        createUserIfAbsent("Risk Analyst", "analyst@example.com", "Analyst@123", Role.ANALYST);

        log.info("Initialized default users: admin@example.com, manager@example.com, analyst@example.com");
    }

    private void createUserIfAbsent(String name, String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .enabled(true)
                    .build());
        }
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

