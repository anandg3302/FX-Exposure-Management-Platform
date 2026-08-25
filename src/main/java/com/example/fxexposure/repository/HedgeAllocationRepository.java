package com.example.fxexposure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.entity.HedgeAllocation;
import com.example.fxexposure.entity.HedgeContract;

@Repository
public interface HedgeAllocationRepository extends JpaRepository<HedgeAllocation, Long> {

    List<HedgeAllocation> findByExposure(Exposure exposure);

    List<HedgeAllocation> findByHedgeContract(HedgeContract hedgeContract);

    List<HedgeAllocation> findByExposureId(Long exposureId);

    List<HedgeAllocation> findByHedgeContractId(Long hedgeContractId);

    void deleteByExposureId(Long exposureId);

    void deleteByHedgeContractId(Long hedgeContractId);
}

