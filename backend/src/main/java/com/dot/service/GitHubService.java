package com.dot.service;

import com.dot.entity.Retrospect;
import com.dot.entity.User;
import com.dot.repository.RetrospectRepository;
import com.dot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final RetrospectRepository retrospectRepository;

    public void updateGitHubSettings(Long userId, String token, String repo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.setGithubToken(token);
        user.setGithubRepo(repo);
        userRepository.save(user);
    }

    public void pushRetroToGitHub(Long userId, Long retrospectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getGithubToken() == null || user.getGithubRepo() == null) {
            throw new RuntimeException("GitHub 설정이 필요합니다. 설정에서 토큰과 저장소를 입력하세요.");
        }

        Retrospect retro = retrospectRepository.findById(retrospectId)
                .orElseThrow(() -> new RuntimeException("회고를 찾을 수 없습니다."));

        // "owner/repo" 형식 파싱
        String repoSetting = user.getGithubRepo().trim();
        String owner, repo;
        if (repoSetting.contains("/")) {
            String[] parts = repoSetting.split("/", 2);
            owner = parts[0];
            repo = parts[1];
        } else {
            owner = getGitHubUsername(user.getGithubToken());
            repo = repoSetting;
        }

        String filePath = "retrospects/" + retro.getDate() + ".md";
        String content = buildMarkdown(retro);
        String sha = getFileSha(user.getGithubToken(), owner, repo, filePath);

        pushFile(user.getGithubToken(), owner, repo, filePath, content, sha, retro.getDate().toString());
    }

    @SuppressWarnings("unchecked")
    private String getGitHubUsername(String token) {
        Map<String, Object> response = webClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("GitHub 사용자 정보를 가져올 수 없습니다.");
        return (String) response.get("login");
    }

    @SuppressWarnings("unchecked")
    private String getFileSha(String token, String owner, String repo, String path) {
        try {
            // URI 템플릿 대신 문자열로 직접 조립 (슬래시 인코딩 방지)
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null ? (String) response.get("sha") : null;
        } catch (WebClientResponseException.NotFound e) {
            return null; // 파일이 아직 없음 → 새로 생성
        } catch (Exception e) {
            log.warn("파일 SHA 조회 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void pushFile(String token, String owner, String repo, String path,
                          String content, String sha, String date) {
        String encoded = Base64.getEncoder()
                .encodeToString(content.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("message", "회고 업로드: " + date);
        body.put("content", encoded);
        if (sha != null) body.put("sha", sha); // 업데이트 시 필요

        // URI 템플릿 대신 문자열로 직접 조립 (슬래시 인코딩 방지)
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path;
        webClient.put()
                .uri(url)
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private String buildMarkdown(Retrospect retro) {
        String stars = "⭐".repeat(retro.getScore());
        return String.format("""
                # 회고 - %s

                **만족도:** %s (%d/5)

                ## ✅ Keep — 잘한 점

                %s

                ## ❗ Problem — 개선할 점

                %s

                ## 🎯 Try — 다음 시도

                %s

                ---
                *DOT 회고 노트에서 작성*
                """,
                retro.getDate(),
                stars, retro.getScore(),
                retro.getKeep() != null ? retro.getKeep() : "(없음)",
                retro.getProblem() != null ? retro.getProblem() : "(없음)",
                retro.getTryContent() != null ? retro.getTryContent() : "(없음)"
        );
    }
}
