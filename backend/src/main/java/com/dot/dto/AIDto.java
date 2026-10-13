package com.dot.dto;

import lombok.Getter;
import lombok.Builder;

import java.util.List;

public class AIDto {

    @Getter
    public static class TryRequest {
        private String keep;
        private String problem;
    }

    @Getter
    @Builder
    public static class TryResponse {
        private List<String> suggestions;
    }

    @Getter
    public static class GuideRequest {
        private String field; // "keep", "problem", "try"
    }

    @Getter
    @Builder
    public static class GuideResponse {
        private String guide;
    }
}
