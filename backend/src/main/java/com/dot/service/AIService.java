package com.dot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final WebClient webClient;

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.model}")
    private String model;

    private static final String GOOGLE_AI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    /**
     * K, P를 바탕으로 T 자동 제안
     */
    public String suggestTry(String keep, String problem) {
        String prompt = """
                당신은 개발자의 회고(KPT)를 돕는 코치입니다.

                아래는 오늘의 Keep(잘한 점)과 Problem(개선할 점)입니다.

                [Keep]
                %s

                [Problem]
                %s

                위 내용을 바탕으로 Try(다음에 시도할 것) 3가지를 구체적이고 실행 가능하게 제안해주세요.
                각 항목은 번호를 붙여 간결하게 작성하고, 실제 행동으로 옮길 수 있는 내용이어야 합니다.
                """.formatted(
                keep != null ? keep : "(없음)",
                problem != null ? problem : "(없음)"
        );

        return callGemma(prompt);
    }

    /**
     * K/P/T 작성 가이드 제공
     */
    public String getWritingGuide(String field) {
        String prompt = switch (field.toLowerCase()) {
            case "keep" -> """
                    KPT 회고의 Keep(잘한 점)을 효과적으로 작성하는 방법을 안내해주세요.
                    좋은 예시와 나쁜 예시를 포함해서, 개발자가 실제로 활용할 수 있도록 간결하게 설명해주세요.
                    """;
            case "problem" -> """
                    KPT 회고의 Problem(개선할 점)을 효과적으로 작성하는 방법을 안내해주세요.
                    문제를 구체적으로 파악하고 원인까지 기술하는 좋은 예시와 나쁜 예시를 포함해주세요.
                    """;
            case "try" -> """
                    KPT 회고의 Try(다음 시도)를 효과적으로 작성하는 방법을 안내해주세요.
                    실행 가능하고 측정 가능한 Try를 작성하는 방법을 예시와 함께 설명해주세요.
                    """;
            default -> "KPT 회고 전체 작성 방법을 간결하게 안내해주세요.";
        };

        return callGemma(prompt);
    }

    @SuppressWarnings("unchecked")
    private String callGemma(String prompt) {
        try {
            // Native Gemini/Gemma API 형식
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 1000,
                            "temperature", 0.7
                    )
            );

            String url = GOOGLE_AI_BASE_URL + model + ":generateContent";

            // AQ. 로 시작하면 AI Studio OAuth 토큰 → Bearer 헤더
            // AIza 로 시작하면 Cloud Console API 키 → query param
            var requestSpec = webClient.post()
                    .uri(apiKey.startsWith("AQ.") ? url : url + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON);

            if (apiKey.startsWith("AQ.")) {
                requestSpec = requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }

            Map<String, Object> response = requestSpec
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) throw new RuntimeException("응답 없음");

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> resContent = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) resContent.get("parts");
            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            log.error("Google AI Studio API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 오류가 발생했습니다.");
        }
    }
}
