package com.dot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TryRecommendationDto {

    @Getter
    @Setter
    public static class Request {
        @NotNull
        private LocalDate periodStart;
        @NotNull
        private LocalDate periodEnd;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private List<String> recommendations;
        private LocalDateTime createdAt;
    }
}
