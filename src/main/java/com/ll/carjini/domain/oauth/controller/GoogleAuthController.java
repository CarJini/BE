package com.ll.carjini.domain.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.ll.carjini.config.GoogleProperties;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.oauth.entity.OAuth2UserInfo;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.domain.oauth.service.CustomOAuth2UserService;
import com.ll.carjini.domain.oauth.token.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class GoogleAuthController {
    private final GoogleProperties googleProperties;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final TokenProvider tokenProvider;


    @GetMapping("/api/auth/login/google")
    public void googleLogin(@RequestParam("redirectUrl") String redirectUrl,
                            HttpServletResponse response) throws IOException {
        Map<String, String> stateObj = new HashMap<>();
        stateObj.put("redirectUrl", redirectUrl);

        String stateJson = objectMapper.writeValueAsString(stateObj);
        String stateParam = Base64.getUrlEncoder().encodeToString(stateJson.getBytes());

        String googleAuthUrl = UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "profile email")
                .queryParam("access_type", "offline")
                .queryParam("state", stateParam)
                .build()
                .toUriString();

        response.sendRedirect(googleAuthUrl);
    }

    // ✅ Android (Mobile) 전용 ID Token 처리
    @PostMapping("/api/auth/google/mobile")
    @ResponseBody
    public ResponseEntity<?> authenticateMobileUser(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance()
            ).setAudience(googleProperties.getClientIds())
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            Map<String, Object> attributes = new HashMap<>(payload);

            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of("google", attributes);
            Member member = customOAuth2UserService.getOrSave(oAuth2UserInfo);

            PrincipalDetails principalDetails = new PrincipalDetails(member, attributes, "sub");
            Authentication authentication = new OAuth2AuthenticationToken(
                    principalDetails,
                    principalDetails.getAuthorities(),
                    "google"
            );

            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication, accessToken);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);

            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed");
        }
    }

    @GetMapping("/auth/google/redirect")
    public void googleCallback(@RequestParam("code") String code,
                               @RequestParam("state") String state,
                               HttpServletResponse response) throws IOException {
        try {
            String decodedState = new String(Base64.getUrlDecoder().decode(state));
            Map<String, String> stateObj = objectMapper.readValue(decodedState, Map.class);
            String redirectUrl = stateObj.get("redirectUrl");

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("code", code);
            requestBody.put("client_id", googleClientId);
            requestBody.put("client_secret", googleClientSecret);
            requestBody.put("redirect_uri", googleRedirectUrl);
            requestBody.put("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token",
                    requestEntity,
                    Map.class
            );

            String googleAccessToken = (String) tokenResponse.get("access_token");
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(googleAccessToken);
            HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);

            Map<String, Object> userInfo = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    userInfoRequest,
                    Map.class
            ).getBody();

            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfo.of("google", userInfo);

            Member member = customOAuth2UserService.getOrSave(oAuth2UserInfo);

            PrincipalDetails principalDetails = new PrincipalDetails(member, userInfo, "sub");
            Authentication authentication = new OAuth2AuthenticationToken(
                    principalDetails,
                    principalDetails.getAuthorities(),
                    "google"
            );


            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication, accessToken);

            String finalRedirectUrl = UriComponentsBuilder
                    .fromUriString(redirectUrl)
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build()
                    .toUriString();

            response.sendRedirect(finalRedirectUrl);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/login?error=" + e.getMessage());
        }
    }
}
