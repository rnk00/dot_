package com.dot.controller;

import com.dot.dto.PatternAnalysisDto;
import com.dot.dto.QuotaDto;
import com.dot.entity.AiFeature;
import com.dot.service.AiQuotaService;
import com.dot.service.PatternAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pattern-analyses")
@RequiredArgsConstructor
public class PatternAnalysisController {

    private static final int DAILY_LIMIT = 10;

    private final PatternAnalysisService patternAnalysisService;
    private final AiQuotaService aiQuotaService;

    @GetMapping
    public ResponseEntity<List<PatternAnalysisDto.Response>> list(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(patternAnalysisService.list(userId));
    }

    @GetMapping("/quota")
    public ResponseEntity<QuotaDto.Response> quota(@AuthenticationPrincipal Long userId) {
        int remaining = aiQuotaService.remaining(userId, AiFeature.PATTERN_ANALYSIS, DAILY_LIMIT);
        return ResponseEntity.ok(QuotaDto.Response.builder().remaining(remaining).limit(DAILY_LIMIT).build());
    }

    @PostMapping
    public ResponseEntity<PatternAnalysisDto.Response> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PatternAnalysisDto.Request request) {
        var result = patternAnalysisService.generate(userId, request.getPeriodStart(), request.getPeriodEnd());
        return ResponseEntity.status(201).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        patternAnalysisService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
