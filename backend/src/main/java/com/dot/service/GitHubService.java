package com.dot.service;

import com.dot.entity.KptItem;
import com.dot.entity.Retrospect;
import com.dot.entity.User;
import com.dot.repository.KptItemRepository;
import com.dot.repository.RetrospectRepository;
import com.dot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final RetrospectRepository retrospectRepository;
    private final KptItemRepository kptItemRepository;

    public void updateGitHubSettings(Long userId, String token, String repo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 둘 다 비어 있으면 연동 해제
        if (isBlank(token) && isBlank(repo)) {
            user.setGithubToken(null);
            user.setGithubRepo(null);
            userRepository.save(user);
            return;
        }
        if (isBlank(token) || isBlank(repo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토큰과 저장소 이름을 모두 입력해주세요.");
        }

        String trimmedToken = token.trim();
        String repoSetting = normalizeRepo(repo);
        try {
            String[] ownerRepo = resolveOwnerRepo(trimmedToken, repoSetting);
            verifyRepoAccess(trimmedToken, ownerRepo[0], ownerRepo[1]);
            user.setGithubToken(trimmedToken);
            user.setGithubRepo(ownerRepo[0] + "/" + ownerRepo[1]); // 확인된 owner/repo 형태로 저장
            userRepository.save(user);
        } catch (WebClientResponseException e) {
            throw toFriendlyException(e, repoSetting);
        }
    }

    /** 토큰으로 저장소에 접근 가능한지, 쓰기 권한이 있는지 확인 */
    @SuppressWarnings("unchecked")
    private void verifyRepoAccess(String token, String owner, String repo) {
        ResponseEntity<Map> response = webClient.get()
                .uri("https://api.github.com/repos/" + owner + "/" + repo)
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .toEntity(Map.class)
                .block();

        if (response == null || response.getBody() == null) {
            throw new RuntimeException("GitHub 저장소 정보를 가져올 수 없습니다.");
        }

        // Classic 토큰이면 X-OAuth-Scopes 헤더로 쓰기 범위 확인 (Fine-grained 토큰엔 이 헤더가 없음)
        String scopes = response.getHeaders().getFirst("X-OAuth-Scopes");
        if (scopes != null && !scopes.contains("repo")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GitHub 토큰에 저장소 쓰기 권한이 없습니다.");
        }

        Map<String, Object> permissions = (Map<String, Object>) response.getBody().get("permissions");
        if (permissions != null && Boolean.FALSE.equals(permissions.get("push"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이 저장소에 쓰기 권한이 없습니다. 본인 저장소인지, 토큰 권한이 Contents: Read and write인지 확인해주세요.");
        }
    }

    public void pushRetroToGitHub(Long userId, Long retrospectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (isBlank(user.getGithubToken()) || isBlank(user.getGithubRepo())) {
            throw new RuntimeException("GitHub 설정이 필요합니다. 설정에서 토큰과 저장소를 입력하세요.");
        }

        Retrospect retro = retrospectRepository.findById(retrospectId)
                .orElseThrow(() -> new RuntimeException("회고를 찾을 수 없습니다."));

        String repoSetting = normalizeRepo(user.getGithubRepo());
        String token = user.getGithubToken().trim();

        try {
            String[] ownerRepo = resolveOwnerRepo(token, repoSetting);
            String owner = ownerRepo[0], repo = ownerRepo[1];

            String filePath = "retrospects/" + retro.getDate() + ".md";
            String content = buildMarkdown(retro);
            String sha = getFileSha(token, owner, repo, filePath);

            pushFile(token, owner, repo, filePath, content, sha, retro.getDate().toString());
        } catch (WebClientResponseException e) {
            throw toFriendlyException(e, repoSetting);
        }

        retro.setIsGithubSynced(true);
        retrospectRepository.save(retro);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** "https://github.com/owner/repo(.git)" 형태로 넣어도 owner/repo 또는 repo만 남김 */
    private String normalizeRepo(String repo) {
        return repo.trim()
                .replaceFirst("^https?://github\\.com/", "")
                .replaceFirst("\\.git$", "")
                .replaceAll("/+$", "");
    }

    /** "owner/repo"면 그대로 나누고, "repo"만 있으면 토큰 주인 계정을 owner로 사용 */
    private String[] resolveOwnerRepo(String token, String repoSetting) {
        if (repoSetting.contains("/")) {
            return repoSetting.split("/", 2);
        }
        return new String[] { getGitHubUsername(token), repoSetting };
    }

    private RuntimeException toFriendlyException(WebClientResponseException e, String repoSetting) {
        log.error("GitHub API 호출 실패 [{}] repo={}: {}", e.getStatusCode(), repoSetting, e.getResponseBodyAsString());
        String reason = switch (e.getStatusCode().value()) {
            case 401 -> "GitHub 토큰이 유효하지 않거나 만료되었습니다. 토큰을 다시 확인해주세요.";
            case 403 -> "GitHub 토큰에 저장소 쓰기 권한(Contents: Read and write)이 없습니다.";
            case 404 -> "GitHub 저장소(" + repoSetting + ")를 찾을 수 없습니다. 저장소 이름과, 토큰 발급 때 이 저장소를 선택했는지 확인해주세요.";
            case 409, 422 -> "GitHub 파일 업데이트 충돌이 발생했습니다. 다시 시도해주세요.";
            default -> "GitHub 요청에 실패했습니다.";
        };
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
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
                renderItems(retro.getId(), KptItem.Type.KEEP),
                renderItems(retro.getId(), KptItem.Type.PROBLEM),
                renderItems(retro.getId(), KptItem.Type.TRY)
        );
    }

    private String renderItems(Long retrospectId, KptItem.Type type) {
        List<KptItem> items = kptItemRepository.findByRetrospectIdAndTypeOrderByOrderIndexAsc(retrospectId, type);
        if (items.isEmpty()) return "(없음)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i + 1).append(". ").append(items.get(i).getContent()).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
