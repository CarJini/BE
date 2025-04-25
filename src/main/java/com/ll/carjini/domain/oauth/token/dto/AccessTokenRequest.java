package com.ll.carjini.domain.oauth.token.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessTokenRequest {
    private String refreshToken;
}
