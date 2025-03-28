package com.ll.carjini.config.security;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.oauth.token.TokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//어쩌구
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final TokenProvider tokenProvider;

    @Value("${app.client.url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication, accessToken);

        Cookie accessCookie = new Cookie("_hoauth", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(false);
        accessCookie.setPath("/");
        accessCookie.setDomain("carjini.shop");
        accessCookie.setMaxAge(3600);

        Cookie refreshCookie = new Cookie("_hrauth", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false);
        refreshCookie.setPath("/");
        refreshCookie.setDomain("carjini.shop");
        refreshCookie.setMaxAge(3600);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);

        response.sendRedirect(redirectUrl);
    }
}
