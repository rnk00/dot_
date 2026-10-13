package com.dot.controller;

import com.dot.entity.User;
import com.dot.repository.AiUsageCounterRepository;
import com.dot.repository.TrySuggestionRepository;
import com.dot.repository.UserRepository;
import com.dot.security.JwtUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final TrySuggestionRepository trySuggestionRepository;
    private final AiUsageCounterRepository aiUsageCounterRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    // 계정 삭제 — User 삭제 시 회고/KPT 항목까지 cascade로 함께 삭제됨 (GitHub 파일은 그대로 둠).
    // AI 추천 히스토리·사용 횟수는 User와 연관관계가 없으므로 따로 지움
    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        trySuggestionRepository.deleteByUserId(userId);
        aiUsageCounterRepository.deleteByUserId(userId);
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Builder
    public static class UserResponse {
        private Long id;
        private String email;
        private String nickname;
        private String profileImage;
        private String provider;
        private String githubRepo;
        private boolean githubConnected;

        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .provider(user.getProvider().name())
                    .githubRepo(user.getGithubRepo())
                    .githubConnected(StringUtils.hasText(user.getGithubToken()) && StringUtils.hasText(user.getGithubRepo()))
                    .build();
        }
    }
}
