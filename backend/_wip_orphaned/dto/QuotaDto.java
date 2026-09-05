package com.dot.dto;

import lombok.Builder;
import lombok.Getter;

public class QuotaDto {

    @Getter
    @Builder
    public static class Response {
        private int remaining;
        private int limit;
    }
}
