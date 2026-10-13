package com.dot.controller;

import com.dot.dto.AIDto;
import com.dot.entity.AiUsageCounter.Feature;
import com.dot.service.AIService;
import com.dot.service.AiQuotaService;
import com.dot.service.TrySuggestionService;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    // AI 기능별 하루 사용 횟수 (한국 시간 자정 초기화). AI를 부르는 엔드포인트는 전부 여기 등록된 제한을 거침
    private static final Map<Feature, Integer> DAILY_LIMITS = Map.of(
            Feature.TRY_RECOMMENDATION, 5,
            Feature.INSIGHT, 3,
            Feature.REFINE, 10
    );

    private final AIService aiService;
    private final AiQuotaService aiQuotaService;
    private final TrySuggestionService trySuggestionService;

    // T 자동 제안 — 성공했을 때만 횟수 차감
    @PostMapping("/suggest-try")
    public ResponseEntity<AIDto.TryResponse> suggestTry(
            @AuthenticationPrincipal Long userId,
            @RequestBody AIDto.TryRequest request) {
        int limit = DAILY_LIMITS.get(Feature.TRY_RECOMMENDATION);
        aiQuotaService.checkAvailable(userId, Feature.TRY_RECOMMENDATION, limit);
        List<String> suggestions = aiService.suggestTry(request.getKeep(), request.getProblem());
        int remaining = aiQuotaService.consume(userId, Feature.TRY_RECOMMENDATION, limit);
        // 회고 날짜가 오면 히스토리로 저장 (작성 기간이 지난 날짜는 어차피 추가할 수 없으니 저장 안 함)
        AIDto.SuggestionBatch batch = request.getDate() != null && TrySuggestionService.isWritable(request.getDate())
                ? trySuggestionService.save(userId, request.getDate(), suggestions)
                : null;
        return ResponseEntity.ok(AIDto.TryResponse.builder()
                .suggestions(suggestions).batch(batch).remaining(remaining).limit(limit).build());
    }

    // 회고 날짜별 Try 추천 히스토리 (최신 묶음부터)
    @GetMapping("/suggest-try/history")
    public ResponseEntity<List<AIDto.SuggestionBatch>> suggestTryHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(trySuggestionService.history(userId, date));
    }

    // K/P/T 한 줄 다듬기 — 성공했을 때만 횟수 차감
    @PostMapping("/refine")
    public ResponseEntity<AIDto.RefineResponse> refine(
            @AuthenticationPrincipal Long userId,
            @RequestBody AIDto.RefineRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다듬을 내용을 먼저 입력해주세요.");
        }
        if (request.getContent().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 번에 500자까지 다듬을 수 있어요.");
        }
        int limit = DAILY_LIMITS.get(Feature.REFINE);
        aiQuotaService.checkAvailable(userId, Feature.REFINE, limit);
        String refined = aiService.refine(request.getType(), request.getContent(), request.getContext());
        int remaining = aiQuotaService.consume(userId, Feature.REFINE, limit);
        return ResponseEntity.ok(AIDto.RefineResponse.builder()
                .refined(refined).remaining(remaining).limit(limit).build());
    }

    // 여러 날 K/P에서 반복 패턴 인사이트 — 성공했을 때만 횟수 차감
    @PostMapping("/insight")
    public ResponseEntity<AIDto.InsightResponse> insight(
            @AuthenticationPrincipal Long userId,
            @RequestBody AIDto.TryRequest request) {
        int limit = DAILY_LIMITS.get(Feature.INSIGHT);
        aiQuotaService.checkAvailable(userId, Feature.INSIGHT, limit);
        String insight = aiService.insight(request.getKeep(), request.getProblem());
        int remaining = aiQuotaService.consume(userId, Feature.INSIGHT, limit);
        return ResponseEntity.ok(AIDto.InsightResponse.builder()
                .insight(insight).remaining(remaining).limit(limit).build());
    }

    // 오늘 남은 횟수 (기능별)
    @GetMapping("/quota")
    public ResponseEntity<Map<Feature, AIDto.QuotaResponse>> quota(@AuthenticationPrincipal Long userId) {
        Map<Feature, AIDto.QuotaResponse> result = new LinkedHashMap<>();
        for (Feature f : Feature.values()) {
            int limit = DAILY_LIMITS.get(f);
            result.put(f, AIDto.QuotaResponse.builder()
                    .remaining(aiQuotaService.remaining(userId, f, limit)).limit(limit).build());
        }
        return ResponseEntity.ok(result);
    }
}
