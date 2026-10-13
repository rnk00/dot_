package com.dot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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
                - 모든 줄은 "~한다"로 끝나는 평서문으로 씁니다. (예: 오전 9시엔 폰을 서랍에 넣어둔다)
                - "~하기", "~하세요", "~해보자" 같은 말투는 쓰지 않습니다.
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
     * K/P/T 항목 한 줄을 작성 가이드 공식에 맞게 다듬음 (프론트 kptGuide.js의 formula와 같은 구조)
     * @param type    KEEP | PROBLEM | TRY
     * @param context 같은 날 쓴 다른 K/P 항목 (원인·맥락을 추측하지 않고 여기서만 가져오도록)
     */
    public String refine(String type, String content, String context) {
        String rule = switch (type == null ? "" : type.toUpperCase()) {
            case "KEEP" -> """
                    Keep(잘한 점)입니다. "무엇을 했고 → 그래서 뭐가 좋아졌는지" 구조로 다듬으세요.
                    잘된 이유가 드러나야 다음에 다시 써먹을 수 있습니다.""";
            case "PROBLEM" -> """
                    Problem(아쉬운 점)입니다. "무슨 일이 있었고 → 왜 그랬고 → 그래서 뭘 놓쳤는지" 구조로 다듬으세요.
                    자책하는 말투 대신 관찰하듯 담담하게 씁니다.""";
            case "TRY" -> """
                    Try(다음에 해볼 것)입니다. "언제 + 무엇을 + 어떻게" 구조로, 내일 했다/안 했다로 확인할 수 있는 행동으로 다듬으세요.
                    다짐("~하자", "열심히")이 아니라 구체적인 행동으로 씁니다.""";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다듬을 항목 종류가 올바르지 않습니다.");
        };

        String prompt = """
                당신은 KPT 회고 문장을 다듬어 주는 편집자입니다.

                %s

                [다듬을 문장]
                %s

                [같은 날 쓴 다른 기록 — 참고용]
                %s

                규칙:
                - 원래 문장과 참고 기록에 있는 사실만 사용하고, 없는 사실(숫자, 도구 이름, 사람 등)은 지어내지 않습니다.
                - 정보가 부족하면 억지로 채우지 말고, 있는 내용 안에서 구조와 표현만 선명하게 만듭니다.
                - 한 문장, 80자 이내, 원래 문장의 말투(~했다/~한다)를 따릅니다.
                - 따옴표, 번호, 마크다운, 설명 없이 다듬은 문장만 출력합니다.
                """.formatted(rule, content.strip(), isBlankText(context) ? "(없음)" : context);

        List<String> lines = parseLines(callGemma(prompt));
        if (lines.isEmpty()) {
            throw new RuntimeException("AI가 문장을 다듬지 못했습니다. 잠시 후 다시 시도해주세요.");
        }
        return lines.get(0).replaceAll("^[\"'“‘]|[\"'”’]$", "");
    }

    /**
     * 여러 날의 K/P 기록을 보고 반복되는 패턴을 짚어주는 인사이트
     */
    public String insight(String keep, String problem) {
        String prompt = """
                당신은 개발자의 회고(KPT)를 돕는 코치입니다.

                아래는 최근 여러 날 동안 기록한 Keep(잘한 점)과 Problem(아쉬운 점)입니다.

                [Keep]
                %s

                [Problem]
                %s

                이 기록에서 반복되는 패턴을 찾아 인사이트를 주세요.

                출력 규칙:
                - 정확히 3줄, 한 줄에 하나씩 씁니다.
                - 1줄: 꾸준히 잘하고 있는 점 / 2줄: 반복되는 문제 / 3줄: 가장 먼저 바꿔볼 한 가지
                - 각 줄은 한두 문장, 기록에 있는 내용을 근거로 구체적으로 씁니다.
                - 번호, 글머리 기호, 마크다운, 인사말은 쓰지 않습니다.
                """.formatted(
                isBlankText(keep) ? "(없음)" : keep,
                isBlankText(problem) ? "(없음)" : problem
        );
        return String.join("\n", parseLines(callGemma(prompt)));
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
