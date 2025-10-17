package com.example.backend.services;

import com.example.backend.models.RefreshToken;
import com.example.backend.models.User;
import com.example.backend.repositories.RefreshTokenRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    /**
     * Crea un nuovo refresh token per l'utente
     */
    @Transactional
    public RefreshToken creaRefreshToken(Long userId) {
        // Invalida vecchi token dell'utente
        refreshTokenRepository.deleteByUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato"));

        // Crea nuovo refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verifica e restituisce un refresh token se valido
     */
    public Optional<RefreshToken> verificaRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isExpired());
    }

    /**
     * Elimina un refresh token
     */
    @Transactional
    public void eliminaRefreshToken(String token) {
        System.out.println(token);
        refreshTokenRepository.findByToken(token)
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
