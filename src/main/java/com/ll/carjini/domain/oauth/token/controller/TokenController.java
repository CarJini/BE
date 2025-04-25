package com.ll.carjini.domain.oauth.token.controller;

import com.ll.carjini.domain.oauth.token.TokenProvider;
import com.ll.carjini.domain.oauth.token.dto.AccessTokenRequest;
import com.ll.carjini.global.dto.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
public class TokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/api/auth/token/refresh")
    public GlobalResponse<String> getToken(@RequestBody AccessTokenRequest request) {
        String accessToken = tokenProvider.generateAccessTokenFromRefreshToken(request.getRefreshToken());

        return GlobalResponse.success(accessToken);
    }
}


