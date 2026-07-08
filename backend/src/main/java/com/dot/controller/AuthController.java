package com.dot.controller;

import com.dot.entity.User;
import com.dot.repository.UserRepository;
import com.dot.security.JwtUtil;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return ResponseEntity.ok(UserResponse.from(user));
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
                    .githubConnected(user.getGithubToken() != null && user.getGithubRepo() != null)
                    .build();
        }
    }
}
