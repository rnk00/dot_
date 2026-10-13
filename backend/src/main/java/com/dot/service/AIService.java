package com.dot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    public List<String> suggestTry(String keep, String problem) {
        String prompt = """
                당신은 개발자의 회고(KPT)를 돕는 코치입니다.

                아래는 오늘의 Keep(잘한 점)과 Problem(개선할 점)입니다.

                [Keep]
                %s

                [Problem]
                %s

                위 내용을 바탕으로 Try(다음에 시도할 것) 3가지를 구체적이고 실행 가능하게 제안해주세요.

                출력 규칙:
                - 정확히 3줄만 출력하고, 한 줄에 Try 하나씩 씁니다.
                - 각 줄은 한 문장, 60자 이내로 실제 행동으로 옮길 수 있게 씁니다.
                - 번호, 글머리 기호, 굵게(**) 같은 마크다운, 제목, 설명, 인사말은 쓰지 않습니다.
                """.formatted(
                isBlankText(keep) ? "(없음)" : keep,
                isBlankText(problem) ? "(없음)" : problem
        );

        List<String> items = parseLines(callGemma(prompt));
        if (items.isEmpty()) {
            throw new RuntimeException("AI가 답변을 만들지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        return items;
    }

    private static boolean isBlankText(String s) {
        return s == null || s.isBlank();
    }

    /** 모델이 규칙을 어기고 번호·기호·마크다운을 붙여도 항목 텍스트만 뽑아냄 */
    private List<String> parseLines(String text) {
        return text.lines()
                .map(String::strip)
                .map(l -> l.replaceFirst("^(\\d+[.)]|[-*•])\\s*", "")) // "1. " "2) " "- " "• " 제거
                .map(l -> l.replace("**", "").replace("__", ""))
                .map(String::strip)
                .filter(l -> !l.isEmpty())
                .filter(l -> !l.endsWith(":")) // "Try 제안:" 같은 머리말 제거
                .limit(3)
                .toList();
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
                            // 최신 Gemini 모델은 생각(thinking) 토큰도 이 한도에 포함돼서, 너무 작으면 본문이 비어서 옴
                            "maxOutputTokens", 4096,
                            "temperature", 0.7
                    )
            );

            // 공식 문서 기준: x-goog-api-key 헤더 사용
            String url = GOOGLE_AI_BASE_URL + model + ":generateContent";

            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) throw new RuntimeException("응답 없음");

            String text = extractText(response);
            if (text == null || text.isBlank()) {
                log.error("Google AI 응답에 본문이 없음: {}", response);
                throw new RuntimeException("AI가 답변을 만들지 못했습니다. 잠시 후 다시 시도해주세요.");
            }
            return text.strip();

        } catch (WebClientResponseException e) {
            log.error("Google AI Studio API 호출 실패 [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            String reason = switch (e.getStatusCode().value()) {
                case 400 -> "AI API 키 또는 요청 형식을 확인해주세요.";
                case 403 -> e.getResponseBodyAsString().contains("denied access")
                        ? "Google이 이 프로젝트의 Gemini API 사용을 차단했습니다. 다른 프로젝트에서 API 키를 발급해주세요."
                        : "AI API 키에 권한이 없습니다. 키와 프로젝트 설정을 확인해주세요.";
                case 404 -> "AI 모델(" + model + ")을 찾을 수 없습니다.";
                case 429 -> "AI 사용량 한도를 초과했습니다. 잠시 후 다시 시도해주세요.";
                default -> "AI 서비스 오류가 발생했습니다.";
            };
            throw new RuntimeException(reason);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google AI Studio API 호출 실패", e);
            throw new RuntimeException("AI 서비스 오류가 발생했습니다.");
        }
    }

    /** candidates[0].content.parts 중 생각(thought) 파트를 제외한 text를 모두 이어붙임 */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;
        Map<String, Object> resContent = (Map<String, Object>) candidates.get(0).get("content");
        if (resContent == null) return null;
        List<Map<String, Object>> parts = (List<Map<String, Object>>) resContent.get("parts");
        if (parts == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> p : parts) {
            if (Boolean.TRUE.equals(p.get("thought"))) continue;
            Object t = p.get("text");
            if (t != null) sb.append(t);
        }
        return sb.toString();
    }
}
