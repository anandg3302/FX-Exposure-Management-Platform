package com.example.fxexposure.service;

import java.time.LocalDate;
import java.util.List;

import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.ExposureResponse;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;

public interface ExposureService {

    ExposureResponse createExposure(ExposureRequest request);

    ExposureResponse updateExposure(Long id, ExposureRequest request);

    void deleteExposure(Long id);

    ExposureResponse getExposureById(Long id);

    Exposure getExposureEntity(Long id);

    List<ExposureResponse> searchExposures(String currency, ExposureStatus status, ExposureType type, LocalDate startDate, LocalDate endDate);

    List<ExposureResponse> getAllExposures();

    void updateExposureHedgingState(Exposure exposure);

    ExposureResponse mapToResponse(Exposure exposure);
}

