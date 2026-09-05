package com.dot.controller;

import com.dot.dto.StatisticsDto;
import com.dot.service.RetrospectService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final RetrospectService retrospectService;

    @GetMapping("/api/statistics")
    public ResponseEntity<StatisticsDto.Response> getStatistics(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        return ResponseEntity.ok(StatisticsDto.Response.builder()
                .scoreTrend(retrospectService.getScoreTrend(userId, periodStart, periodEnd))
                .build());
    }
}
