package com.hl7decoder.api;

import com.hl7decoder.api.dto.Hl7Request;
import com.hl7decoder.model.Hl7ParseResult;
import com.hl7decoder.model.platform.Hl7RepairResponse;
import com.hl7decoder.service.Hl7RepairService;
import com.hl7decoder.service.Hl7Service;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hl7")
public class Hl7Controller {
    private final Hl7Service hl7Service;
    private final Hl7RepairService hl7RepairService;

    public Hl7Controller(Hl7Service hl7Service, Hl7RepairService hl7RepairService) {
        this.hl7Service = hl7Service;
        this.hl7RepairService = hl7RepairService;
    }

    @PostMapping("/parse")
    public Hl7ParseResult parse(@Valid @RequestBody Hl7Request request) {
        return hl7Service.parseAndValidate(request);
    }

    @PostMapping("/validate")
    public Hl7ParseResult validate(@Valid @RequestBody Hl7Request request) {
        return hl7Service.parseAndValidate(request);
    }

    @PostMapping("/repair")
    public Hl7RepairResponse repair(@Valid @RequestBody Hl7Request request) {
        return hl7RepairService.repair(request);
    }
}
