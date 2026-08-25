package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.dto.RiskPolicyRequest;
import com.example.fxexposure.dto.RiskPolicyResponse;

public interface RiskPolicyService {

    RiskPolicyResponse createPolicy(RiskPolicyRequest request);

    RiskPolicyResponse updatePolicy(Long id, RiskPolicyRequest request);

    void deletePolicy(Long id);

    List<RiskPolicyResponse> getActivePolicies();

    List<RiskPolicyResponse> getAllPolicies();

    RiskPolicyResponse getPolicyById(Long id);

    List<ComplianceAlertResponse> checkCompliance();

    void seedDefaultPolicies();
}

