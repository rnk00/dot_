package com.dot.controller;

import com.dot.service.GitHubService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;

    /** GitHub 토큰 + 저장소 저장 */
    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(
            @AuthenticationPrincipal Long userId,
            @RequestBody GitHubSettingsRequest request) {
        gitHubService.updateGitHubSettings(userId, request.getToken(), request.getRepo());
        return ResponseEntity.ok().build();
    }

    /** 회고 → GitHub 업로드 */
    @PostMapping("/push/{retrospectId}")
    public ResponseEntity<Void> pushRetro(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long retrospectId) {
        gitHubService.pushRetroToGitHub(userId, retrospectId);
        return ResponseEntity.ok().build();
    }

    @Data
    static class GitHubSettingsRequest {
        private String token;
        private String repo;
    }
}
