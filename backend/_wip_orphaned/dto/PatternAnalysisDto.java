package com.dot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PatternAnalysisDto {

    @Getter
    @Setter
    public static class Request {
        @NotNull
        private LocalDate periodStart;
        @NotNull
        private LocalDate periodEnd;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordItem {
        private String keyword;
        private Integer count;
        private String comment;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private List<KeywordItem> keywords;
        private String warningPattern;
        private LocalDateTime createdAt;
    }
}
