package com.dot.oauth2;

import com.dot.entity.User;
import com.dot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            String registrationId = userRequest.getClientRegistration().getRegistrationId();

            OAuth2UserInfo userInfo = extractUserInfo(registrationId, oAuth2User.getAttributes());
            User user = saveOrUpdate(userInfo, registrationId);

            return new CustomOAuth2User(user, oAuth2User.getAttributes());
        } catch (Exception e) {
            log.error("OAuth2 로그인 처리 중 오류", e);
            throw e;
        }
    }

    private OAuth2UserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> KakaoUserInfo.from(attributes);
            case "github" -> GithubUserInfo.from(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }

    private User saveOrUpdate(OAuth2UserInfo info, String registrationId) {
        User.Provider provider = User.Provider.valueOf(registrationId.toUpperCase());

        return userRepository.findByProviderAndProviderId(provider, info.getProviderId())
                .map(user -> {
                    user.setNickname(info.getNickname());
                    user.setProfileImage(info.getProfileImage());
                    return userRepository.save(user);
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(info.getEmail())
                                .nickname(info.getNickname())
                                .profileImage(info.getProfileImage())
                                .provider(provider)
                                .providerId(info.getProviderId())
                                .build()
                ));
    }
}
