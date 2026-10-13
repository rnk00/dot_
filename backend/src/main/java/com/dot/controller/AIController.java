package com.dot.controller;

import com.dot.dto.AIDto;
import com.dot.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    // T 자동 제안
    @PostMapping("/suggest-try")
    public ResponseEntity<AIDto.TryResponse> suggestTry(
            @AuthenticationPrincipal Long userId,
            @RequestBody AIDto.TryRequest request) {
        List<String> suggestions = aiService.suggestTry(request.getKeep(), request.getProblem());
        return ResponseEntity.ok(AIDto.TryResponse.builder().suggestions(suggestions).build());
    }

    // 작성 가이드
    @GetMapping("/guide/{field}")
    public ResponseEntity<AIDto.GuideResponse> getGuide(
            @AuthenticationPrincipal Long userId,
            @PathVariable String field) {
        String guide = aiService.getWritingGuide(field);
        return ResponseEntity.ok(AIDto.GuideResponse.builder().guide(guide).build());
    }
}
