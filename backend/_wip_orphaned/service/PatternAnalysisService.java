package com.dot.service;

import com.dot.dto.PatternAnalysisDto;
import com.dot.entity.AiFeature;
import com.dot.entity.KptItem;
import com.dot.entity.KptType;
import com.dot.entity.PatternAnalysis;
import com.dot.exception.ApiException;
import com.dot.repository.KptItemRepository;
import com.dot.repository.PatternAnalysisRepository;
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
public class PatternAnalysisService {

    private static final int DAILY_LIMIT = 10;

    private final PatternAnalysisRepository patternAnalysisRepository;
    private final KptItemRepository kptItemRepository;
    private final AIService aiService;
    private final AiQuotaService aiQuotaService;
    private final ObjectMapper objectMapper;

    public List<PatternAnalysisDto.Response> list(Long userId) {
        return patternAnalysisRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        PatternAnalysis analysis = patternAnalysisRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("분석 결과를 찾을 수 없습니다."));
        if (!analysis.getUserId().equals(userId)) {
            throw ApiException.forbidden("권한이 없습니다.");
        }
        patternAnalysisRepository.delete(analysis);
    }

    @Transactional
    public PatternAnalysisDto.Response generate(Long userId, LocalDate periodStart, LocalDate periodEnd) {
        List<KptItem> items = kptItemRepository.findByRetrospect_User_IdAndTypeInAndRetrospect_DateBetween(
                userId, List.of(KptType.KEEP, KptType.PROBLEM), periodStart, periodEnd);

        if (items.isEmpty()) {
            throw ApiException.badRequest("해당 기간에 작성한 회고가 없어요.");
        }

        aiQuotaService.consume(userId, AiFeature.PATTERN_ANALYSIS, DAILY_LIMIT);

        String prompt = buildPrompt(items);
        String raw = aiService.generateText(prompt);

        ParsedResult parsed = parse(raw);

        PatternAnalysis analysis = PatternAnalysis.builder()
                .userId(userId)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .keywordsJson(writeKeywords(parsed.keywords))
                .warningPattern(parsed.warningPattern)
                .build();

        return toResponse(patternAnalysisRepository.save(analysis));
    }

    private String buildPrompt(List<KptItem> items) {
        StringBuilder sb = new StringBuilder();
        for (KptItem item : items) {
            sb.append("- [").append(item.getType() == KptType.KEEP ? "Keep" : "Problem").append("] ")
                    .append(item.getContent()).append("\n");
        }

        return """
                아래는 개발자가 기간 동안 작성한 회고의 Keep(잘한 점)/Problem(아쉬운 점) 목록입니다.

                [목록]
                %s

                비슷한 의미의 표현을 하나로 묶어서, 자주 언급된 순서대로 최대 8개의 키워드를 뽑아주세요.
                각 키워드에는 몇 번 언급됐는지와, 그 키워드에 대한 짧은 코멘트(한 문장, 조언이나 통찰)를 붙여주세요.
                그리고 Problem 중 가장 반복적으로 나타나는 패턴을 한 문장으로 요약해주세요(없으면 빈 문자열).

                반드시 아래 JSON 형식으로만, 다른 설명 없이 응답하세요:
                {"keywords":[{"keyword":"문자열","count":정수,"comment":"문자열"}],"warning_pattern":"문자열"}
                """.formatted(sb);
    }

    private ParsedResult parse(String raw) {
        try {
            String json = stripCodeFence(raw);
            var node = objectMapper.readTree(json);
            List<PatternAnalysisDto.KeywordItem> keywords = new java.util.ArrayList<>();
            for (var kwNode : node.path("keywords")) {
                keywords.add(PatternAnalysisDto.KeywordItem.builder()
                        .keyword(kwNode.path("keyword").asText())
                        .count(kwNode.path("count").asInt(0))
                        .comment(kwNode.path("comment").asText(""))
                        .build());
            }
            String warningPattern = node.path("warning_pattern").asText("");
            return new ParsedResult(keywords, warningPattern);
        } catch (Exception e) {
            log.error("패턴 분석 응답 파싱 실패: {}", e.getMessage());
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

    private String writeKeywords(List<PatternAnalysisDto.KeywordItem> keywords) {
        try {
            return objectMapper.writeValueAsString(keywords);
        } catch (Exception e) {
            throw ApiException.badRequest("AI 응답을 처리하지 못했습니다. 다시 시도해주세요.");
        }
    }

    private List<PatternAnalysisDto.KeywordItem> readKeywords(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PatternAnalysisDto.KeywordItem.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private PatternAnalysisDto.Response toResponse(PatternAnalysis analysis) {
        return PatternAnalysisDto.Response.builder()
                .id(analysis.getId())
                .periodStart(analysis.getPeriodStart())
                .periodEnd(analysis.getPeriodEnd())
                .keywords(readKeywords(analysis.getKeywordsJson()))
                .warningPattern(analysis.getWarningPattern())
                .createdAt(analysis.getCreatedAt())
                .build();
    }

    // 내부 파싱 결과 보관용
    private record ParsedResult(List<PatternAnalysisDto.KeywordItem> keywords, String warningPattern) {}
}
