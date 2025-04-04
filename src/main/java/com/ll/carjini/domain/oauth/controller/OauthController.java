package com.ll.carjini.domain.oauth.controller;

import com.ll.carjini.domain.oauth.token.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class OauthController {

    private final TokenProvider tokenProvider;

    @GetMapping("/login/google")
    public ResponseEntity<Map<String, String>> getLoginUrl(
            @RequestParam(required = false) String redirectUrl,
            HttpSession session
    ) {
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            session.setAttribute("OAUTH2_REDIRECT_URL", redirectUrl);
        }

        String loginUrl = "/oauth2/authorization/google";

        Map<String, String> response = new HashMap<>();
        response.put("url", loginUrl);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/login/status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(
            @CookieValue(name = "_hoauth", required = false) String accessToken
    ) {
        Map<String, Object> response = new HashMap<>();
        boolean isLoggedIn = accessToken != null;
        response.put("isLoggedIn", isLoggedIn);

        return ResponseEntity.ok(response);
    }
}