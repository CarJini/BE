package com.ll.carjini.domain.oauth.token.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Token {
    private String accessToken;
    private String refreshToken;
    private String username;

    public Token(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
