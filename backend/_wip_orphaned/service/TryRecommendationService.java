package com.dot.service;

import com.dot.dto.TryRecommendationDto;
import com.dot.entity.AiFeature;
import com.dot.entity.KptItem;
import com.dot.entity.KptType;
import com.dot.entity.PatternAnalysis;
import com.dot.entity.TryRecommendation;
import com.dot.exception.ApiException;
import com.dot.repository.KptItemRepository;
import com.dot.repository.PatternAnalysisRepository;
import com.dot.repository.RetrospectRepository;
import com.dot.repository.TryRecommendationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TryRecommendationService {

    private static final int DAILY_LIMIT = 5;

    private final TryRecommendationRepository tryRecommendationRepository;
    private final PatternAnalysisRepository patternAnalysisRepository;
    private final KptItemRepository kptItemRepository;
    private final RetrospectRepository retrospectRepository;
    private final AIService aiService;
    private final AiQuotaService aiQuotaService;
    private final PatternAnalysisService patternAnalysisService;
    private final ObjectMapper objectMapper;

    public List<TryRecommendationDto.Response> list(Long userId) {
        return tryRecommendationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        TryRecommendation rec = tryRecommendationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("추천 결과를 찾을 수 없습니다."));
        if (!rec.getUserId().equals(userId)) {
            throw ApiException.forbidden("권한이 없습니다.");
        }
        tryRecommendationRepository.delete(rec);
    }

    @Transactional
    public TryRecommendationDto.Response generate(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        if (retrospectRepository.findByUserIdAndDateBetween(userId, periodStart, periodEnd).isEmpty()) {
            throw ApiException.badRequest("해당 기간에 작성한 회고가 없어요.");
        }

        aiQuotaService.consume(userId, AiFeature.TRY_RECOMMENDATION, DAILY_LIMIT);

        // 같은 기간의 패턴 분석 결과가 있으면 재사용, 없으면 새로 생성(패턴 분석 하루 한도에도 포함됨)
        PatternAnalysis basis = patternAnalysisRepository
                .findFirstByUserIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(userId, periodStart, periodEnd)
                .orElseGet(() -> {
                    patternAnalysisService.generate(userId, periodStart, periodEnd);
                    return patternAnalysisRepository
                            .findFirstByUserIdAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(userId, periodStart, periodEnd)
                            .orElseThrow(() -> ApiException.badRequest("AI 응답을 처리하지 못했습니다. 다시 시도해주세요."));
                });

        List<KptItem> existingTries = kptItemRepository.findByRetrospect_User_IdAndTypeAndRetrospect_DateBetween(
                userId, KptType.TRY, periodStart, periodEnd);

        String prompt = buildPrompt(basis, existingTries);
        String raw = aiService.generateText(prompt);
        List<String> recommendations = parse(raw);

        TryRecommendation rec = TryRecommendation.builder()
                .userId(userId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .recommendationsJson(write(recommendations))
                .build();

        return toResponse(tryRecommendationRepository.save(rec));
    }

    private String buildPrompt(PatternAnalysis basis, List<KptItem> existingTries) {
        String existing = existingTries.isEmpty() ? "(없음)"
                : existingTries.stream().map(i -> "- " + i.getContent()).reduce("", (a, b) -> a + b + "\n");

        return """
                아래는 개발자의 기간별 회고에서 반복적으로 나타난 문제 패턴입니다.

                [반복된 문제 패턴]
                %s

                [이미 작성된 Try 목록 (참고만 하고 반복하지 마세요)]
                %s

                위 문제 패턴을 해결하기 위한 새로운 Try(다음에 시도할 것) 3가지를 구체적이고 실행 가능하게 제안해주세요.
                기존 Try 목록과 겹치지 않는 새로운 제안이어야 하며, 마크다운 문법 기호(*, #, - 등)는 쓰지 마세요.

                반드시 아래 JSON 배열 형식으로만, 다른 설명 없이 응답하세요:
                ["문장1", "문장2", "문장3"]
                """.formatted(
                (basis.getWarningPattern() == null || basis.getWarningPattern().isBlank())
                        ? "(뚜렷한 반복 패턴 없음)" : basis.getWarningPattern(),
                existing
        );
    }

    private List<String> parse(String raw) {
        try {
            String json = stripCodeFence(raw);
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Try 추천 응답 파싱 실패: {}", e.getMessage());
            throw ApiException.badRequest("AI 응답을 처리하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String write(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw ApiException.badRequest("AI 응답을 처리하지 못했습니다. 다시 시도해주세요.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> read(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private TryRecommendationDto.Response toResponse(TryRecommendation rec) {
        return TryRecommendationDto.Response.builder()
                .id(rec.getId())
                .periodStart(rec.getPeriodStart())
                .periodEnd(rec.getPeriodEnd())
                .recommendations(read(rec.getRecommendationsJson()))
                .createdAt(rec.getCreatedAt())
                .build();
    }
}
