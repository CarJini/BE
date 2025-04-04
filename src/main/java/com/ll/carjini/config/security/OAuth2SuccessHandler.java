package com.ll.carjini.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.oauth.token.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//어쩌구
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;

    @Value("${app.client.url}")
    private String defaultRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication, accessToken);

        System.out.println("OAuth2 인증 성공: " + authentication.getName());
        System.out.println("액세스 토큰 발급 완료: " + accessToken.substring(0, 10) + "...");

        String targetUrl = defaultRedirectUrl;
        HttpSession session = request.getSession(false);
        if (session != null) {
            String savedRedirectUrl = (String) session.getAttribute("OAUTH2_REDIRECT_URL");
            if (savedRedirectUrl != null && !savedRedirectUrl.isEmpty()) {
                targetUrl = savedRedirectUrl;
                session.removeAttribute("OAUTH2_REDIRECT_URL");
            }
        }

        // Check if redirect URL has query parameters
        String delimiter = targetUrl.contains("?") ? "&" : "?";

        // Add tokens to the redirect URL
        String finalRedirectUrl = targetUrl + delimiter +
                "accessToken=" + accessToken +
                "&refreshToken=" + refreshToken;

        // Redirect to the client application with tokens
        response.sendRedirect(finalRedirectUrl);
    }
}
