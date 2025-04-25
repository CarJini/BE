package com.ll.carjini.domain.oauth.token.service;

import com.ll.carjini.domain.oauth.token.entity.Token;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token.expire-time}")
    private long refreshTokenExpireTime;

    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveOrUpdate(String memberId, String refreshToken, String accessToken) {
        redisTemplate.opsForValue().set(memberId + ":refresh-token", refreshToken, Duration.ofSeconds(refreshTokenExpireTime));
        redisTemplate.opsForValue().set(memberId + ":access-token", accessToken);
    }

    public void deleteToken(String username) {
        redisTemplate.delete(username + ":refresh-token");
        redisTemplate.delete(username + ":access-token");
    }
}