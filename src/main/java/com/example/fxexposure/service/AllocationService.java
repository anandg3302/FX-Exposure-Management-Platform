package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.dto.AllocationRequest;
import com.example.fxexposure.dto.AllocationResponse;

public interface AllocationService {

    AllocationResponse allocateHedgeToExposure(AllocationRequest request);

    void deallocate(Long allocationId);

    List<AllocationResponse> getAllocationsForExposure(Long exposureId);

    List<AllocationResponse> getAllocationsForHedge(Long hedgeContractId);

    List<AllocationResponse> getAllAllocations();
}

