package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.dto.AuthResponse;
import com.example.fxexposure.dto.LoginRequest;
import com.example.fxexposure.dto.RegisterRequest;
import com.example.fxexposure.dto.UserDto;

public interface UserService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserDto getCurrentUser();

    List<UserDto> getAllUsers();

    UserDto updateUserRole(Long id, com.example.fxexposure.enums.Role role);

    UserDto toggleUserStatus(Long id);

    void seedDefaultUsers();
}

