package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoveragePolicySourceRepository extends JpaRepository<CoveragePolicySourceEntity, Long> {
    List<CoveragePolicySourceEntity> findTop25ByPayerContainingIgnoreCaseOrPolicyIdContainingIgnoreCaseOrTitleContainingIgnoreCase(String payer, String policyId, String title);

    void deleteByImportJobId(String importJobId);
}
