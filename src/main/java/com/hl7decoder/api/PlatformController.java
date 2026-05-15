package com.hl7decoder.api;

import com.hl7decoder.api.dto.platform.MedicalNecessityRequest;
import com.hl7decoder.api.dto.platform.SyntheticDataRequest;
import com.hl7decoder.api.dto.platform.TextRequest;
import com.hl7decoder.model.platform.FhirConversionResponse;
import com.hl7decoder.model.platform.MedicalNecessityResponse;
import com.hl7decoder.model.platform.SyntheticDataResponse;
import com.hl7decoder.model.platform.X12DecodeResponse;
import com.hl7decoder.service.FhirConversionService;
import com.hl7decoder.service.MedicalNecessityService;
import com.hl7decoder.service.PlatformRoadmapService;
import com.hl7decoder.service.SyntheticDataService;
import com.hl7decoder.service.X12Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/platform", "/api/v1/platform"})
public class PlatformController {
    private final FhirConversionService fhirConversionService;
    private final SyntheticDataService syntheticDataService;
    private final X12Service x12Service;
    private final MedicalNecessityService medicalNecessityService;
    private final PlatformRoadmapService platformRoadmapService;

    public PlatformController(
            FhirConversionService fhirConversionService,
            SyntheticDataService syntheticDataService,
            X12Service x12Service,
            MedicalNecessityService medicalNecessityService,
            PlatformRoadmapService platformRoadmapService
    ) {
        this.fhirConversionService = fhirConversionService;
        this.syntheticDataService = syntheticDataService;
        this.x12Service = x12Service;
        this.medicalNecessityService = medicalNecessityService;
        this.platformRoadmapService = platformRoadmapService;
    }

    @PostMapping("/fhir/hl7-to-fhir")
    public FhirConversionResponse hl7ToFhir(@Valid @RequestBody TextRequest request) {
        return fhirConversionService.hl7ToFhir(request.text());
    }

    @PostMapping("/fhir/fhir-to-hl7")
    public java.util.Map<String, Object> fhirToHl7(@Valid @RequestBody TextRequest request) {
        return fhirConversionService.fhirToHl7(request.text());
    }

    @PostMapping("/hl7/profile-validate")
    public java.util.Map<String, Object> profileValidate(@Valid @RequestBody com.hl7decoder.api.dto.Hl7Request request) {
        return platformRoadmapService.profileValidate(request.message(), request.mode());
    }

    @PostMapping("/hl7/deep-analysis")
    public java.util.Map<String, Object> hl7DeepAnalysis(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.hl7DeepAnalysis(request.text());
    }

    @PostMapping("/fhir/mapping-template")
    public java.util.Map<String, Object> mappingTemplate(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.mappingTemplate(request.text());
    }

    @PostMapping("/fhir/validate")
    public java.util.Map<String, Object> validateFhir(@Valid @RequestBody TextRequest request) {
        return fhirConversionService.validate(request.text(), "FHIR_R4_CORE");
    }

    @PostMapping("/fhir/batch-hl7-to-fhir")
    public java.util.Map<String, Object> batchHl7ToFhir(@Valid @RequestBody TextRequest request) {
        return fhirConversionService.batchHl7ToFhir(request.text());
    }

    @PostMapping("/synthetic/generate")
    public SyntheticDataResponse generateSyntheticData(@RequestBody SyntheticDataRequest request) {
        return syntheticDataService.generate(request);
    }

    @PostMapping("/synthetic/export-manifest")
    public java.util.Map<String, Object> syntheticExportManifest() {
        return platformRoadmapService.syntheticExportManifest();
    }

    @PostMapping("/x12/decode")
    public X12DecodeResponse decodeX12(@Valid @RequestBody TextRequest request) {
        return x12Service.decode(request.text());
    }

    @PostMapping("/x12/revenue-cycle")
    public java.util.Map<String, Object> revenueCycle(@Valid @RequestBody TextRequest request) {
        return x12Service.revenueCycle(request.text());
    }

    @PostMapping("/x12/generate-270")
    public java.util.Map<String, Object> generate270(@Valid @RequestBody TextRequest request) {
        return x12Service.generate270(request.text());
    }

    @PostMapping("/necessity/check")
    public MedicalNecessityResponse checkMedicalNecessity(@Valid @RequestBody MedicalNecessityRequest request) {
        return medicalNecessityService.check(request);
    }

    @PostMapping("/prior-auth/analyze")
    public java.util.Map<String, Object> priorAuth(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.priorAuth(request.text());
    }

    @PostMapping("/denials/analyze")
    public java.util.Map<String, Object> denials(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.denial(request.text());
    }

    @PostMapping("/cdi/analyze")
    public java.util.Map<String, Object> cdi(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.cdi(request.text());
    }

    @PostMapping("/terminology/normalize")
    public java.util.Map<String, Object> terminology(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.terminology(request.text());
    }

    @PostMapping("/labs/interpret")
    public java.util.Map<String, Object> labs(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.labInterpret(request.text());
    }

    @PostMapping("/monitoring/snapshot")
    public java.util.Map<String, Object> monitoring(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.monitoring(request.text());
    }

    @PostMapping("/coding/assist")
    public java.util.Map<String, Object> coding(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.coding(request.text());
    }

    @PostMapping("/sandbox/plan")
    public java.util.Map<String, Object> sandbox(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.sandbox(request.text());
    }

    @PostMapping("/eligibility/analyze")
    public java.util.Map<String, Object> eligibility(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.eligibility(request.text());
    }

    @PostMapping("/payer/requirements")
    public java.util.Map<String, Object> payerRequirements(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.payerRequirements(request.text());
    }

    @PostMapping("/compliance/scan")
    public java.util.Map<String, Object> compliance(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.compliance(request.text());
    }

    @PostMapping("/search")
    public java.util.Map<String, Object> globalSearch(@Valid @RequestBody TextRequest request) {
        return platformRoadmapService.globalSearch(request.text());
    }
}
