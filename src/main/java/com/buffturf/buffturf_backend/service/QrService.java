package com.buffturf.buffturf_backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class QrService {

    @Value("${app.jwt.secret:BuffTURFSecretKey2024VeryLongAndSecureKeyForJWTSigning123456}")
    private String jwtSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateQrToken(Long participantId, Long bookingId, Date expiresAt) {
        return Jwts.builder()
                .setSubject("qr-pass")
                .claim("participantId", participantId)
                .claim("bookingId", bookingId)
                .setIssuedAt(new Date())
                .setExpiration(expiresAt)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateQrToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null; // Invalid token or expired
        }
    }
}
