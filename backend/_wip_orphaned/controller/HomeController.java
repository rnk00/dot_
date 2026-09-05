package com.dot.controller;

import com.dot.dto.RetrospectDto;
import com.dot.service.RetrospectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final RetrospectService retrospectService;

    // 캘린더(홈) 사이드바: 최근 회고 3개 + streak
    @GetMapping("/api/home-summary")
    public ResponseEntity<RetrospectDto.HomeSummaryResponse> getHomeSummary(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(retrospectService.getHomeSummary(userId));
    }
}
