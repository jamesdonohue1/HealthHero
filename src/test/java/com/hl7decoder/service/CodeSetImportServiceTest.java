package com.hl7decoder.service;

import com.hl7decoder.api.dto.admin.CodeSetImportRequest;
import com.hl7decoder.persistence.CodeSetImportJob;
import com.hl7decoder.persistence.CodeSetImportJobRepository;
import com.hl7decoder.persistence.CodeSynonymRepository;
import com.hl7decoder.persistence.CoveragePolicySourceRepository;
import com.hl7decoder.persistence.Icd10CodeEntity;
import com.hl7decoder.persistence.Icd10CodeRepository;
import com.hl7decoder.persistence.IcdCptRuleRepository;
import com.hl7decoder.persistence.ProcedureCodeRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CodeSetImportServiceTest {
    @Test
    void importsLargeIcdCodeSetFileWithValidationSummary() {
        CodeSetImportJobRepository jobRepository = repository(CodeSetImportJobRepository.class);
        Icd10CodeRepository icd10Repository = repository(Icd10CodeRepository.class);
        CodeSetImportService service = new CodeSetImportService(
                jobRepository,
                icd10Repository,
                repository(ProcedureCodeRepository.class),
                repository(CodeSynonymRepository.class),
                repository(IcdCptRuleRepository.class),
                repository(CoveragePolicySourceRepository.class)
        );
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            content.append("R05.").append(i % 10).append(",Cough,Cough long,Symptoms,2026-10-01,,true,,CMS,provenance,0.9\n");
        }

        var response = service.importContent(new CodeSetImportRequest(
                "ICD10_CM",
                "2026",
                "large fixture",
                LocalDate.parse("2026-10-01"),
                true,
                content.toString()
        ), UUID.randomUUID(), UUID.randomUUID());

        assertThat(response.totalRows()).isEqualTo(300);
        assertThat(response.importedRows()).isEqualTo(300);
        assertThat(response.rejectedRows()).isZero();
        assertThat(response.validationSummary()).contains("Imported 300 rows");
    }

    private <T> T repository(Class<T> type) {
        Object proxy = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (target, method, args) -> {
                    if ("save".equals(method.getName())) {
                        return args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "InMemory" + type.getSimpleName();
                    }
                    return null;
                }
        );
        return type.cast(proxy);
    }
}
