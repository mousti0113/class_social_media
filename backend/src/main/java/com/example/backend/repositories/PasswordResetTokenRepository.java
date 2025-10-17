package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    // Trova per token
    Optional<PasswordResetToken> findByToken(String token);
    
    // Trova token valido
    @Query("""
        SELECT prt FROM PasswordResetToken prt
        WHERE prt.token = :token 
        AND prt.used = false 
        AND prt.expiresAt > :now
        """)
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    // Invalida vecchi token di un utente
    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user.id = :userId AND prt.used = false")
    void invalidateUserTokens(@Param("userId") Long userId);
    
    // Elimina token scaduti o usati
    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :now OR prt.used = true")
    void deleteExpiredOrUsedTokens(@Param("now") LocalDateTime now);
}
