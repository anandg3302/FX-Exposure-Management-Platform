package com.example.fxexposure.dto;

import com.example.fxexposure.enums.AlertStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertStatusUpdateRequest {

    @NotNull(message = "Alert status is required")
    private AlertStatus status;
}

