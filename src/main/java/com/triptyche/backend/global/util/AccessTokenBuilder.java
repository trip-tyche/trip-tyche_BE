package com.triptyche.backend.global.util;

import com.triptyche.backend.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessTokenBuilder {

    private final JwtSecretKeyManager jwtSecretKeyManager;
    private final JwtProperties jwtProperties;

    public String build(String email, List<String> roles, String provider) {
        return build(email, roles, provider, null);
    }

    /**
     * sessionId는 sid로 실린다. access 토큰만 들고 오는 앱에서도
     * 어느 로그인 세션인지 특정할 수 있어야 로그아웃이 그 세션만 끊는다.
     */
    public String build(String email, List<String> roles, String provider, String sessionId) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("roles", roles);
        claims.put("provider", provider);
        if (sessionId != null) {
            claims.put("sid", sessionId);
        }

        Date now = new Date();
        Date validity = new Date(now.getTime() + jwtProperties.accessTokenExpirySeconds() * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(jwtSecretKeyManager.getSecretKey(provider), SignatureAlgorithm.HS256)
                .compact();
    }
}
