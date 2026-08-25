package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.UserDto;
import com.example.fxexposure.enums.Role;
import com.example.fxexposure.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing users and roles (ADMIN / MANAGER only)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List all users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(@PathVariable Long id, @RequestParam Role role) {
        UserDto user = userService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", user));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable user account (ADMIN only)")
    public ResponseEntity<ApiResponse<UserDto>> toggleUserStatus(@PathVariable Long id) {
        UserDto user = userService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", user));
    }
}

