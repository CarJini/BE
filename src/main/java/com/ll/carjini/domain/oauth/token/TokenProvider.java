package com.ll.carjini.domain.oauth.token;

import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.repository.MemberRepository;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.oauth.token.service.TokenService;
import com.ll.carjini.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@Slf4j
public class TokenProvider {

    @Value("${jwt.key}")
    private String key;
    private SecretKey secretKey;
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 2;
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000 * 60 * 60L * 24 * 7;
    private static final String KEY_ROLE = "role";
    private final TokenService tokenService;
    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    private void setSecretKey() {
        secretKey = Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateAccessToken(Authentication authentication) {
        return generateToken(authentication, ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String generateRefreshToken(Authentication authentication, String accessToken) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String memberId = String.valueOf(principalDetails.user().getId());

        String refreshToken = generateToken(authentication, REFRESH_TOKEN_EXPIRE_TIME);
        tokenService.saveOrUpdate(memberId, refreshToken, accessToken);
        return refreshToken;
    }

    private String generateToken(Authentication authentication, long expireTime) {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() + expireTime);

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long memberId = principalDetails.user().getId();
        log.info("memberId 생성 중 : {}", memberId);

        return Jwts.builder()
                .setSubject(memberId.toString())
                .claim(KEY_ROLE, authorities)
                .setIssuedAt(now)
                .setExpiration(expiredDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        Long memberId = Long.parseLong(claims.getSubject());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        PrincipalDetails principalDetails = new PrincipalDetails(
                member,
                new HashMap<>(),
                "memberId"
        );

        return new UsernamePasswordAuthenticationToken(principalDetails, "", principalDetails.getAuthorities());
    }

    private List<SimpleGrantedAuthority> getAuthorities(Claims claims) {
        return Collections.singletonList(new SimpleGrantedAuthority(
                claims.get(KEY_ROLE).toString()));
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            boolean isValid = claims.getExpiration().after(new Date());
            return isValid;
        } catch (Exception e) {
            System.out.println("Error parsing claims: " + e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (MalformedJwtException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (SecurityException e) {
            throw new CustomException(ErrorCode.INVALID_JWT_SIGNATURE);
        }
    }

    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Claims claims = parseClaims(refreshToken);
        String memberId = claims.getSubject();
        String role = claims.get("role", String.class);
        log.info("refreshToken: {}", memberId);

        String storedToken = redisTemplate.opsForValue().get(memberId + ":refresh-token");
        if (!refreshToken.equals(storedToken)) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }

        return Jwts.builder()
                .setSubject(memberId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRE_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
