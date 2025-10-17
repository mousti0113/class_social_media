package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    // Trova per token
    Optional<RefreshToken> findByToken(String token);
    
    // Elimina token di un utente
    void deleteByUserId(Long userId);
    
    // Elimina token scaduti
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    // Verifica esistenza token valido
    @Query("""
        SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END
        FROM RefreshToken rt
        WHERE rt.token = :token AND rt.expiresAt > :now
        """)
    boolean isTokenValid(@Param("token") String token, @Param("now") LocalDateTime now);
}
