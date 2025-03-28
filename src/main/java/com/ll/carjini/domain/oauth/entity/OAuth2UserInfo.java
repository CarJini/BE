package com.ll.carjini.domain.oauth.entity;

import com.ll.carjini.domain.member.entity.AuthProvider;
import lombok.Builder;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.entity.Role;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;

import java.util.Map;

@Builder
public record OAuth2UserInfo(
        String name,
        String email,
        String profile,
        String provider,
        String providerId
) {

    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) { // registration id별로 userInfo 생성
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            default -> throw new CustomException(ErrorCode.ILLEGAL_REGISTRATION_ID);
        };
    }

    private static OAuth2UserInfo ofGoogle(Map<String, Object> attributes) {
        return OAuth2UserInfo.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .profile((String) attributes.get("picture"))
                .provider("GOOGLE")
                .providerId(attributes.get("sub").toString())
                .build();
    }

    private static OAuth2UserInfo ofKakao(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");

        return OAuth2UserInfo.builder()
                .name((String) profile.get("nickname"))
                .email((String) account.get("email"))
                .profile((String) profile.get("profile_image_url"))
                .provider("KAKAO")
                .providerId(attributes.get("id").toString())
                .build();
    }

    public Member toEntity() {
        return Member.builder()
                .name(name)
                .email(email)
                .profile(profile)
                .provider(AuthProvider.valueOf(provider))
                .providerId(providerId)
                .role(Role.USER)
                .build();
    }
}
