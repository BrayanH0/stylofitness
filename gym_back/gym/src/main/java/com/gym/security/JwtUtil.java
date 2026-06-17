package com.gym.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    private final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long dni, String nombre, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("dni", dni);
        claims.put("nombre", nombre);
        claims.put("rol", rol);
        return createToken(claims, String.valueOf(dni));
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String dni) {
        final String extractedDni = extractUsername(token);
        return (extractedDni.equals(dni) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractDni(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("dni", Long.class);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("rol", String.class);
    }

    public String extractNombre(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("nombre", String.class);
    }

    public String generateTempToken(Long dni, long validityMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("dni", dni);
        claims.put("purpose", "post-payment-activation");
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(dni))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateTempToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String purpose = claims.get("purpose", String.class);
            return "post-payment-activation".equals(purpose);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}