package com.ll.demo03.domain.oauth.token.controller;

import com.ll.demo03.domain.oauth.token.TokenProvider;
import com.ll.demo03.domain.oauth.token.dto.AccessTokenRequest;
import com.ll.demo03.domain.oauth.token.dto.AccessTokenResponse;
import com.ll.demo03.domain.oauth.token.service.TokenService;
import com.ll.demo03.global.dto.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RequiredArgsConstructor
@RestController
public class TokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/auth/token/verify")
    public ResponseEntity<GlobalResponse<String>> getToken(@RequestBody AccessTokenRequest request) {
        String accessToken = tokenProvider.generateAccessTokenFromRefreshToken(request.get_hrauth());

        HttpCookie cookie = ResponseCookie.from("_hoauth", accessToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofHours(1))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(GlobalResponse.success(accessToken));
    }
}


