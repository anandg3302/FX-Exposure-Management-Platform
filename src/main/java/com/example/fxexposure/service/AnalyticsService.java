package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.dto.DashboardOverviewDto;
import com.example.fxexposure.dto.MaturityBucketDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.ScenarioAnalysisRequest;
import com.example.fxexposure.dto.ScenarioAnalysisResultDto;
import com.example.fxexposure.dto.VaRResultDto;

public interface AnalyticsService {

    NetExposureSummaryDto getNetExposureSummary();

    List<MaturityBucketDto> getMaturityLadder();

    ScenarioAnalysisResultDto runScenarioStressTest(ScenarioAnalysisRequest request);

    VaRResultDto calculateVaR();

    DashboardOverviewDto getDashboardOverview();
}

