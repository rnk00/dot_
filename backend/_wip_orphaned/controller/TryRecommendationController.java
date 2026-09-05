package com.dot.controller;

import com.dot.dto.QuotaDto;
import com.dot.dto.TryRecommendationDto;
import com.dot.entity.AiFeature;
import com.dot.service.AiQuotaService;
import com.dot.service.TryRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/try-recommendations")
@RequiredArgsConstructor
public class TryRecommendationController {

    private static final int DAILY_LIMIT = 5;

    private final TryRecommendationService tryRecommendationService;
    private final AiQuotaService aiQuotaService;

    @GetMapping
    public ResponseEntity<List<TryRecommendationDto.Response>> list(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(tryRecommendationService.list(userId));
    }

    @GetMapping("/quota")
    public ResponseEntity<QuotaDto.Response> quota(@AuthenticationPrincipal Long userId) {
        int remaining = aiQuotaService.remaining(userId, AiFeature.TRY_RECOMMENDATION, DAILY_LIMIT);
        return ResponseEntity.ok(QuotaDto.Response.builder().remaining(remaining).limit(DAILY_LIMIT).build());
    }

    @PostMapping
    public ResponseEntity<TryRecommendationDto.Response> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TryRecommendationDto.Request request) {
        var result = tryRecommendationService.generate(userId, request.getPeriodStart(), request.getPeriodEnd());
        return ResponseEntity.status(201).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        tryRecommendationService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
