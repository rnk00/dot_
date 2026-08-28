package com.dot.oauth2;

import jakarta.servlet.http.Cookie;
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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * 우리 백엔드는 SessionCreationPolicy.STATELESS라 HTTP 세션을 안 쓰는데, Spring Security의
 * 기본 oauth2Login()은 로그인 진행 중(authorize -> callback 사이) 상태를 세션에 저장해서
 * 콜백에서 "authorization_request_not_found"가 났었다. 세션 대신 쿠키를 썼더니 이번엔
 * OAuth2AuthorizationRequest 전체(직렬화하면 2KB 넘음)를 쿠키 값에 담다 보니 간헐적으로
 * 유실되는 문제가 있었다. 그래서 쿠키에는 최소한의 정보(provider 이름)만 담고, 나머지는
 * ClientRegistrationRepository에서 그때그때 다시 만든다. 또한 쿠키 이름 자체에 state 값을
 * 넣어서, 동시에 여러 로그인 시도가 겹쳐도 서로 다른 쿠키를 쓰게 해 덮어쓰기 경합도 없앤다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_PREFIX = "oauth2_req_";
    private static final int COOKIE_MAX_AGE = 180; // 초 — 로그인 절차 도중에만 필요

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
        if (authorizationRequest == null) return;

        String registrationId = registrationIdOf(authorizationRequest);
        String state = authorizationRequest.getState();
        if (registrationId == null || state == null) return;

        Cookie cookie = new Cookie(cookieName(state), registrationId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(cookie);
        log.info("[oauth2-cookie] save: provider={} state={}", registrationId, shorten(state));
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                   HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = reconstruct(request);
        String state = request.getParameter("state");
        if (state != null) {
            Cookie cookie = new Cookie(cookieName(state), "");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
        return authorizationRequest;
    }

    // 쿠키(있으면 provider 이름 하나)와 콜백 URL의 state 파라미터로부터
    // ClientRegistration 정보를 다시 조회해 OAuth2AuthorizationRequest를 그때그때 새로 만든다.
    private OAuth2AuthorizationRequest reconstruct(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) return null;

        String registrationId = getCookieValue(request, cookieName(state)).orElse(null);
        log.info("[oauth2-cookie] load: state={} found={}", shorten(state), registrationId != null);
        if (registrationId == null) return null;

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (registration == null) return null;

        String redirectUri = backendUrl + "/login/oauth2/code/" + registrationId;

        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(redirectUri)
                .scopes(registration.getScopes())
                .state(state)
                .attributes(Map.of("registration_id", registrationId))
                .build();
    }

    private String registrationIdOf(OAuth2AuthorizationRequest authorizationRequest) {
        Object id = authorizationRequest.getAttributes().get("registration_id");
        return id != null ? id.toString() : null;
    }

    private String cookieName(String state) {
        // state는 base64url이라 대부분 쿠키 이름에 안전하지만, 혹시 모를 문자(=)는 제거
        return COOKIE_PREFIX + state.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private String shorten(String state) {
        return state.length() > 8 ? state.substring(0, 8) + "..." : state;
    }
}
