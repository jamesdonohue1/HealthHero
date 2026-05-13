package com.hl7decoder.api;

import com.hl7decoder.api.dto.cpt.ProcedureSearchRequest;
import com.hl7decoder.model.cpt.ProcedureSearchResponse;
import com.hl7decoder.model.cpt.ProcedureSearchResult;
import com.hl7decoder.service.CptSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cpt")
public class CptSearchController {
    private final CptSearchService cptSearchService;

    public CptSearchController(CptSearchService cptSearchService) {
        this.cptSearchService = cptSearchService;
    }

    @GetMapping("/search")
    public ProcedureSearchResponse search(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "effectiveDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate
    ) {
        return cptSearchService.search(new ProcedureSearchRequest(query, limit, effectiveDate));
    }

    @GetMapping("/{code}")
    public ProcedureSearchResult lookup(@PathVariable String code) {
        return cptSearchService.lookup(code);
    }
}
