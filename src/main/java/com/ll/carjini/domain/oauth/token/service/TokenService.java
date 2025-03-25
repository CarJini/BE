package com.ll.demo03.domain.oauth.token.service;

import com.ll.demo03.domain.oauth.token.TokenProvider;
import com.ll.demo03.domain.oauth.token.entity.Token;
import com.ll.demo03.global.error.ErrorCode;
import com.ll.demo03.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token.expire-time}")
    private long refreshTokenExpireTime;

    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveOrUpdate(String username, String refreshToken, String accessToken) {
        redisTemplate.opsForValue().set(username + ":refresh-token", refreshToken, refreshTokenExpireTime);
        redisTemplate.opsForValue().set(username + ":access-token", accessToken);
    }

    public void updateToken(String newAccessToken, Token token) {
        String username = token.getUsername();
        String newRefreshToken = token.getRefreshToken();

        this.saveOrUpdate(username, newRefreshToken, newAccessToken);
    }


    public Token findByAccessTokenOrThrow(String accessToken) {
        String storedToken = redisTemplate.opsForValue().get(accessToken);

        if (storedToken == null) {
            throw new CustomException(ErrorCode.ACCESS_TOKEN_EXPIRED);
        }
        return new Token(accessToken, storedToken); // 예시로 Token 클래스 사용
    }

    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(username + ":refresh-token");
    }

    public void deleteToken(String username) {
        redisTemplate.delete(username + ":refresh-token");
        redisTemplate.delete(username + ":access-token");
    }
}