package com.ll.carjini.domain.oauth.service;

import com.ll.carjini.domain.member.entity.AuthProvider;
import com.ll.carjini.domain.member.entity.Role;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.repository.MemberRepository;
import com.ll.carjini.domain.oauth.entity.OAuth2UserInfo;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 Login Start"); // 디버깅 로그 추가

        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("Provider: {}", registrationId); // 어떤 OAuth 제공자인지 확인

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of(registrationId, attributes);
        Member member = getOrSave(oAuth2UserInfo);

        log.info("Member found/created: {}", member); // 멤버 정보 확인

        PrincipalDetails principalDetails = new PrincipalDetails(member, attributes, userNameAttributeName);
        log.info("PrincipalDetails created: {}", principalDetails);
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(
                        principalDetails,
                        principalDetails.getAuthorities(),
                        registrationId
                )
        );

        return principalDetails;
    }

    public Member getOrSave(OAuth2UserInfo oAuth2UserInfo) {
        return memberRepository.findByEmail(oAuth2UserInfo.email())
                .map(existingMember -> {
                    // 기존 회원 정보 업데이트가 필요하다면 여기서 수행
                    log.info("Existing member found: {}", existingMember);
                    return existingMember;
                })
                .orElseGet(() -> {
                    Member newMember = oAuth2UserInfo.toEntity();
                    log.info("New member created: {}", newMember);
                    return memberRepository.save(newMember);
                });
    }

}
