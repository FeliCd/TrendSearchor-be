package com.fpt.swp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt-secret:daf66e01593f61a15b857cf433aae03a005812b31234e149036bcc8dee755dbb}")
    private String jwtSecret;

    @Value("${app.jwt-expiration-milliseconds:604800000}") // 7 ngày
    private long jwtExpirationDate;

    /**
     * Sinh JWT với jti (JWT ID) để hỗ trợ blacklist khi logout (FR-01.3).
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationDate);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti claim – định danh duy nhất mỗi token
                .subject(username)
                .issuedAt(currentDate)
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Lấy jti từ token – dùng để lưu vào blacklist khi logout (FR-01.3).
     */
    public String getJti(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Lấy thời điểm hết hạn của token – dùng để set expiresAt trong blacklist.
     */
    public Instant getExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
