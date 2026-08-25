package com.example.fxexposure.dto;

import java.time.LocalDateTime;

import com.example.fxexposure.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
}

