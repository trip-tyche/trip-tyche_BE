package com.triptyche.backend.global.util;

import com.triptyche.backend.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenBuilder {

    private final JwtSecretKeyManager jwtSecretKeyManager;
    private final JwtProperties jwtProperties;

    /**
     * sessionId는 jti로 실린다. 사용자당 refresh 토큰이 하나뿐이던 제약을 푸는 키이자,
     * 같은 초에 발급된 토큰이 바이트 단위로 같아지는 것을 막는 값이다.
     */
    public String build(String email, String provider, String sessionId) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("provider", provider);
        claims.setId(sessionId);

        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.refreshTokenExpirySeconds() * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(jwtSecretKeyManager.getSecretKey(provider), SignatureAlgorithm.HS256)
                .compact();
    }
}
