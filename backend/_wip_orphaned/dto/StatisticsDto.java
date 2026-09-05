package com.dot.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class StatisticsDto {

    @Getter
    @Builder
    public static class Response {
        private List<RetrospectDto.CalendarDayItem> scoreTrend;
    }
}
