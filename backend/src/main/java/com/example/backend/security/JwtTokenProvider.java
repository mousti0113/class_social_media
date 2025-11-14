package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Provider per la gestione dei token JWT.
 * Responsabile di generare, validare e estrarre informazioni dai token.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Genera un access token per l'utente
     */
    public String generateAccessToken(UserDetails userDetails) {
        log.debug("Generazione access token per utente: {}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", "access");

        return generateToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Genera un refresh token per l'utente
     */
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("Generazione refresh token per utente: {}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("tipo", "refresh");

        return generateToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    /**
     * Genera un token JWT con claims personalizzati
     */
    private String generateToken(Map<String, Object> extraClaims, String username, Long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Estrae l'username dal token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Estrae la data di scadenza dal token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Estrae un claim specifico dal token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Estrae tutti i claims dal token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica se il token è scaduto
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Valida il token confrontando username e scadenza
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

            if (isValid) {
                log.debug("Token JWT valido per utente: {}", username);
            } else {
                log.warn("Token JWT non valido per utente: {}", username);
            }

            return isValid;

        } catch (SignatureException e) {
            log.error("Firma JWT non valida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformato: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT scaduto: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT non supportato: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string è vuoto: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Ottiene la chiave di firma dal secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Verifica se il token è valido (non scaduto e ben formato)
     */
    public Boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Token non valido: {}", e.getMessage());
            return false;
        }
    }
}