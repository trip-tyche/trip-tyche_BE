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

    public String build(String email, String provider) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("provider", provider);

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
