package com.hl7decoder.service;

import com.hl7decoder.api.dto.admin.CodeSetImportRequest;
import com.hl7decoder.model.admin.CodeSetImportJobResponse;
import com.hl7decoder.persistence.CodeSetImportJob;
import com.hl7decoder.persistence.CodeSetImportJobRepository;
import com.hl7decoder.persistence.CodeSynonymEntity;
import com.hl7decoder.persistence.CodeSynonymRepository;
import com.hl7decoder.persistence.CoveragePolicySourceEntity;
import com.hl7decoder.persistence.CoveragePolicySourceRepository;
import com.hl7decoder.persistence.Icd10CodeEntity;
import com.hl7decoder.persistence.Icd10CodeRepository;
import com.hl7decoder.persistence.IcdCptRuleEntity;
import com.hl7decoder.persistence.IcdCptRuleRepository;
import com.hl7decoder.persistence.ProcedureCodeEntity;
import com.hl7decoder.persistence.ProcedureCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CodeSetImportService {
    private final CodeSetImportJobRepository jobRepository;
    private final Icd10CodeRepository icd10CodeRepository;
    private final ProcedureCodeRepository procedureCodeRepository;
    private final CodeSynonymRepository synonymRepository;
    private final IcdCptRuleRepository ruleRepository;
    private final CoveragePolicySourceRepository policyRepository;

    public CodeSetImportService(CodeSetImportJobRepository jobRepository,
                                Icd10CodeRepository icd10CodeRepository,
                                ProcedureCodeRepository procedureCodeRepository,
                                CodeSynonymRepository synonymRepository,
                                IcdCptRuleRepository ruleRepository,
                                CoveragePolicySourceRepository policyRepository) {
        this.jobRepository = jobRepository;
        this.icd10CodeRepository = icd10CodeRepository;
        this.procedureCodeRepository = procedureCodeRepository;
        this.synonymRepository = synonymRepository;
        this.ruleRepository = ruleRepository;
        this.policyRepository = policyRepository;
    }

    @Transactional
    public CodeSetImportJobResponse importContent(CodeSetImportRequest request, UUID organizationId, UUID userId) {
        UUID id = UUID.randomUUID();
        CodeSetImportJob job = jobRepository.save(new CodeSetImportJob(id, type(request.codeSetType()), request.codeSetVersion(), request.sourceName()));
        ImportCounters counters = new ImportCounters();
        List<String> messages = new ArrayList<>();
        try {
            List<String> lines = request.content().lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .toList();
            for (String line : lines) {
                counters.total++;
                try {
                    importLine(job, request, line);
                    counters.imported++;
                } catch (RuntimeException ex) {
                    counters.rejected++;
                    if (messages.size() < 20) {
                        messages.add("Line " + counters.total + ": " + ex.getMessage());
                    }
                }
            }
            String summary = messages.isEmpty()
                    ? "Imported " + counters.imported + " rows."
                    : String.join("\n", messages);
            job.complete(counters.total, counters.imported, counters.rejected, summary);
        } catch (RuntimeException ex) {
            job.fail(ex.getMessage());
        }
        return response(job);
    }

    @Transactional(readOnly = true)
    public List<CodeSetImportJobResponse> history() {
        return jobRepository.findTop25ByOrderByCreatedAtDesc().stream().map(this::response).toList();
    }

    @Transactional
    public CodeSetImportJobResponse rollback(UUID id, UUID organizationId, UUID userId) {
        CodeSetImportJob job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Import job not found."));
        String importJobId = id.toString();
        icd10CodeRepository.deleteByImportJobId(importJobId);
        procedureCodeRepository.deleteByImportJobId(importJobId);
        synonymRepository.deleteByImportJobId(importJobId);
        ruleRepository.deleteByImportJobId(importJobId);
        policyRepository.deleteByImportJobId(importJobId);
        job.rollback();
        return response(job);
    }

    private void importLine(CodeSetImportJob job, CodeSetImportRequest request, String line) {
        List<String> columns = split(line);
        String type = job.getCodeSetType();
        if ("ICD10_CM".equals(type)) {
            importIcd10(job, request, columns);
        } else if ("HCPCS".equals(type) || "CPT".equals(type)) {
            importProcedure(job, request, columns, type);
        } else if ("SYNONYM".equals(type)) {
            synonymRepository.save(new CodeSynonymEntity(required(columns, 0), required(columns, 1), required(columns, 2), job.getId().toString()));
        } else if ("PAYER_RULE".equals(type)) {
            importRule(job, request, columns);
        } else if ("LCD_NCD_POLICY".equals(type)) {
            importPolicy(job, request, columns);
        } else {
            throw new IllegalArgumentException("Unsupported code set type: " + type);
        }
    }

    private void importIcd10(CodeSetImportJob job, CodeSetImportRequest request, List<String> columns) {
        String code = required(columns, 0).toUpperCase(Locale.ROOT);
        icd10CodeRepository.save(new Icd10CodeEntity(
                code,
                value(columns, 1),
                value(columns, 2),
                value(columns, 3),
                date(value(columns, 4), request.defaultEffectiveDate()),
                date(value(columns, 5), null),
                bool(value(columns, 6), true),
                value(columns, 7),
                job.getCodeSetVersion(),
                source(request, value(columns, 8)),
                job.getId().toString(),
                value(columns, 9),
                decimal(value(columns, 10), 1.0)
        ));
    }

    private void importProcedure(CodeSetImportJob job, CodeSetImportRequest request, List<String> columns, String type) {
        procedureCodeRepository.save(new ProcedureCodeEntity(
                required(columns, 0).toUpperCase(Locale.ROOT),
                type,
                value(columns, 1),
                value(columns, 2),
                value(columns, 3),
                date(value(columns, 4), request.defaultEffectiveDate()),
                date(value(columns, 5), null),
                bool(value(columns, 6), Boolean.TRUE.equals(request.activate())),
                source(request, value(columns, 7)),
                job.getCodeSetVersion(),
                value(columns, 8),
                job.getId().toString(),
                value(columns, 9),
                decimal(value(columns, 10), 1.0)
        ));
    }

    private void importRule(CodeSetImportJob job, CodeSetImportRequest request, List<String> columns) {
        ruleRepository.save(new IcdCptRuleEntity(
                required(columns, 0).toUpperCase(Locale.ROOT),
                required(columns, 1).toUpperCase(Locale.ROOT),
                value(columns, 2),
                value(columns, 3).isBlank() ? "MEDICAL_NECESSITY" : value(columns, 3),
                value(columns, 4).isBlank() ? "SUPPORTED" : value(columns, 4),
                value(columns, 5),
                source(request, value(columns, 6)),
                date(value(columns, 7), request.defaultEffectiveDate()),
                date(value(columns, 8), null),
                job.getId().toString(),
                value(columns, 9),
                decimal(value(columns, 10), 0.75),
                null
        ));
    }

    private void importPolicy(CodeSetImportJob job, CodeSetImportRequest request, List<String> columns) {
        policyRepository.save(new CoveragePolicySourceEntity(
                required(columns, 0),
                required(columns, 1),
                required(columns, 2),
                value(columns, 3),
                value(columns, 4),
                source(request, value(columns, 5)),
                date(value(columns, 6), request.defaultEffectiveDate()),
                date(value(columns, 7), null),
                job.getId().toString()
        ));
    }

    private CodeSetImportJobResponse response(CodeSetImportJob job) {
        return new CodeSetImportJobResponse(job.getId(), job.getCodeSetType(), job.getCodeSetVersion(), job.getSourceName(),
                job.getStatus(), job.getTotalRows(), job.getImportedRows(), job.getRejectedRows(),
                job.getCreatedAt(), job.getCompletedAt(), job.getRolledBackAt(), job.getValidationSummary());
    }

    private List<String> split(String line) {
        return Arrays.stream(line.split(",", -1)).map(String::trim).toList();
    }

    private String required(List<String> columns, int index) {
        String value = value(columns, index);
        if (value.isBlank()) {
            throw new IllegalArgumentException("column " + (index + 1) + " is required");
        }
        return value;
    }

    private String value(List<String> columns, int index) {
        return index < columns.size() ? columns.get(index).trim() : "";
    }

    private String source(CodeSetImportRequest request, String value) {
        return value == null || value.isBlank() ? request.sourceName() : value;
    }

    private LocalDate date(String value, LocalDate fallback) {
        return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
    }

    private boolean bool(String value, boolean fallback) {
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private double decimal(String value, double fallback) {
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    private String type(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static final class ImportCounters {
        private int total;
        private int imported;
        private int rejected;
    }
}
