package com.hl7decoder.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IcdCptRuleRepository extends JpaRepository<IcdCptRuleEntity, Long> {
    List<IcdCptRuleEntity> findByCptCodeIgnoreCase(String cptCode);

    void deleteByImportJobId(String importJobId);
}
