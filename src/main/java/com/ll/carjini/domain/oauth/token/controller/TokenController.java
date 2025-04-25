package com.ll.carjini.domain.oauth.token.controller;

import com.ll.carjini.domain.oauth.token.TokenProvider;
import com.ll.carjini.domain.oauth.token.dto.AccessTokenRequest;
import com.ll.carjini.global.dto.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@Tag(name = "토큰 API", description = "AccessToken 발급 API")
public class TokenController {

    private final TokenProvider tokenProvider;

    @PostMapping("/api/auth/token/refresh")
    @Operation(summary = "AccessToken 발급", description = "RefreshToken을 통해 AccessToken을 발급합니다.")
    public GlobalResponse<String> getToken(@RequestBody AccessTokenRequest request) {
        String accessToken = tokenProvider.generateAccessTokenFromRefreshToken(request.getRefreshToken());

        return GlobalResponse.success(accessToken);
    }
}


