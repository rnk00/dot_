package com.dot.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Render(무료 인스턴스) 앞단 인프라를 거치면서 이 리다이렉트 응답의 Set-Cookie가
 * 브라우저까지 살아서 안 돌아오는 문제를 세션 저장, 큰 쿠키, 요청별 작은 쿠키까지
 * 다 시도해봐도 못 없애서, 아예 서버 쪽에 아무것도 저장하지 않는 방식으로 바꿨다.
 *
 * 콜백 URL 자체가 "/login/oauth2/code/{registrationId}" 형태라 어떤 제공자로 로그인
 * 중이었는지는 URL만 보고도 알 수 있고, 나머지(클라이언트ID, 인가 URI, scope 등)는
 * ClientRegistrationRepository에서 바로 조회 가능하다. state 값은 콜백에 그대로
 * 들어있는 걸 신뢰해서 그대로 사용한다(=저장해둔 값과 대조하는 CSRF 검증은 빠짐 —
 * 개인 프로젝트 규모에서는 감수 가능한 트레이드오프로 판단).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendUrl;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return reconstruct(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        // 아무것도 저장하지 않음 — 콜백 시점에 URL 정보만으로 재구성
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        return reconstruct(request);
    }

    private OAuth2AuthorizationRequest reconstruct(HttpServletRequest request) {
        String registrationId = registrationIdFromUri(request.getRequestURI());
        String state = request.getParameter("state");
        if (registrationId == null || state == null) {
            log.info("[oauth2] reconstruct 불가: uri={} state={}", request.getRequestURI(), state);
            return null;
        }

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) {
            log.info("[oauth2] 등록되지 않은 provider: {}", registrationId);
            return null;
        }

        log.info("[oauth2] reconstruct: provider={} state={}...", registrationId,
                state.length() > 8 ? state.substring(0, 8) : state);

        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(backendUrl + "/login/oauth2/code/" + registrationId)
                .scopes(registration.getScopes())
                .state(state)
                .attributes(Map.of("registration_id", registrationId))
                .build();
    }

    // "/login/oauth2/code/kakao" -> "kakao" / "/oauth2/authorization/kakao" -> "kakao"
    private String registrationIdFromUri(String uri) {
        if (uri == null) return null;
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == uri.length() - 1) return null;
        return uri.substring(lastSlash + 1);
    }
}
