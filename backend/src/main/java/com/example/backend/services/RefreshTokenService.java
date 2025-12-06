package com.example.backend.services;

import com.example.backend.models.RefreshToken;
import com.example.backend.models.User;
import com.example.backend.repositories.RefreshTokenRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service per la gestione dei refresh token nel database.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Crea un nuovo refresh token per l'utente
     * SICUREZZA: Il token viene hashato con BCrypt prima del salvataggio
     */
    @Transactional
    public RefreshToken creaRefreshToken(Long userId) {
        // Invalida vecchi token dell'utente
        refreshTokenRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Genera token in chiaro (da restituire al client)
        String plainToken = UUID.randomUUID().toString();
        
        // Hash del token per storage sicuro
        String hashedToken = passwordEncoder.encode(plainToken);

        // Crea nuovo refresh token con hash
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashedToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        
        // Imposta temporaneamente il token in chiaro per la risposta
        // (il client ha bisogno del token non hashato)
        saved.setToken(plainToken);
        
        return saved;
    }

    /**
     * Verifica e restituisce un refresh token se valido
     * SICUREZZA: Confronta il token in chiaro con gli hash nel database
     * PERFORMANCE: Usa query ottimizzata per caricare solo token non scaduti
     */
    public Optional<RefreshToken> verificaRefreshToken(String plainToken) {
        // Recupera solo i token non scaduti e cerca match con BCrypt
        return refreshTokenRepository.findAllValid(LocalDateTime.now()).stream()
                .filter(rt -> passwordEncoder.matches(plainToken, rt.getToken()))
                .findFirst();
    }

    /**
     * Elimina un refresh token
     * SICUREZZA: Cerca il token hashato nel database
     * PERFORMANCE: Usa query ottimizzata per caricare solo token non scaduti
     */
    @Transactional
    public void eliminaRefreshToken(String plainToken) {
        // Trova il token che matcha e lo elimina
        refreshTokenRepository.findAllValid(LocalDateTime.now()).stream()
                .filter(rt -> passwordEncoder.matches(plainToken, rt.getToken()))
                .findFirst()
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Elimina tutti i refresh token di un utente
     */
    @Transactional
    public void eliminaRefreshTokenUtente(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Pulizia periodica dei token scaduti (da chiamare con @Scheduled)
     */
    @Transactional
    public void pulisciTokenScaduti() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
