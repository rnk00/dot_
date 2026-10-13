package com.dot.dto;

import lombok.Getter;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AIDto {

    @Getter
    public static class TryRequest {
        private String keep;
        private String problem;
        private LocalDate date; // Try 추천: 어느 날짜 회고의 추천인지 (히스토리 저장용)
    }

    @Getter
    @Builder
    public static class SuggestionBatch {
        private String batchId;
        private LocalDateTime createdAt;
        private List<String> suggestions;
    }

    @Getter
    @Builder
    public static class TryResponse {
        private List<String> suggestions;
        private SuggestionBatch batch; // 히스토리에 저장된 묶음 (date를 보냈을 때)
        private int remaining; // 오늘 남은 추천 횟수
        private int limit;     // 하루 추천 횟수
    }

    @Getter
    @Builder
    public static class QuotaResponse {
        private int remaining;
        private int limit;
    }

    @Getter
    public static class RefineRequest {
        private String type;    // KEEP | PROBLEM | TRY
        private String content; // 다듬을 문장
        private String context; // 같은 날 쓴 다른 K/P (참고용, 선택)
    }

    @Getter
    @Builder
    public static class RefineResponse {
        private String refined;
        private int remaining;
        private int limit;
    }

    @Getter
    @Builder
    public static class InsightResponse {
        private String insight;
        private int remaining;
        private int limit;
    }
}
