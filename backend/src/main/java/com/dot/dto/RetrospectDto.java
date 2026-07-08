package com.dot.dto;

import com.dot.entity.Retrospect;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetrospectDto {

    @Getter
    @Setter
    public static class Request {
        @NotNull
        private LocalDate date;
        private String keep;
        private String problem;
        private String tryContent;

        @Min(1) @Max(5)
        @Builder.Default
        private Integer score = 3;

        private String colorTheme = "default";
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private LocalDate date;
        private String keep;
        private String problem;
        private String tryContent;
        private Integer score;
        private String colorTheme;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(Retrospect r) {
            return Response.builder()
                    .id(r.getId())
                    .date(r.getDate())
                    .keep(r.getKeep())
                    .problem(r.getProblem())
                    .tryContent(r.getTryContent())
                    .score(r.getScore())
                    .colorTheme(r.getColorTheme())
                    .createdAt(r.getCreatedAt())
                    .updatedAt(r.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CalendarItem {
        private Long id;
        private LocalDate date;
        private Integer score;
        private String colorTheme;
    }
}
