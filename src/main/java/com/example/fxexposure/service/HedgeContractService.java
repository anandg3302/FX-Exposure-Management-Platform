package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.HedgeContractResponse;
import com.example.fxexposure.dto.SettlementRequest;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;

public interface HedgeContractService {

    HedgeContractResponse bookHedgeContract(HedgeContractRequest request);

    HedgeContractResponse updateHedgeContract(Long id, HedgeContractRequest request);

    void deleteHedgeContract(Long id);

    HedgeContractResponse getHedgeContractById(Long id);

    HedgeContract getHedgeContractEntity(Long id);

    List<HedgeContractResponse> searchHedges(String currency, HedgeStatus status, HedgeType hedgeType, String counterparty);

    List<HedgeContractResponse> getAllHedgeContracts();

    HedgeContractResponse settleHedgeContract(Long id, SettlementRequest request);

    HedgeContractResponse revalueHedgeContract(Long id);

    void revalueAllActiveHedges();

    HedgeContractResponse mapToResponse(HedgeContract hedge);
}

