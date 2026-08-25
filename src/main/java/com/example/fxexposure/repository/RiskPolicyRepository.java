package com.example.fxexposure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.RiskPolicy;

@Repository
public interface RiskPolicyRepository extends JpaRepository<RiskPolicy, Long> {

    List<RiskPolicy> findByActiveTrue();

    Optional<RiskPolicy> findByCurrencyAndActiveTrue(String currency);

    List<RiskPolicy> findByCurrency(String currency);
}

