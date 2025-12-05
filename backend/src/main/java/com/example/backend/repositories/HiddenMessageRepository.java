package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.models.HiddenMessage;

import java.util.Optional;

@Repository
public interface HiddenMessageRepository extends JpaRepository<HiddenMessage, Long> {

    // Verifica se un messaggio è nascosto per un utente
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    // Trova record specifico
    Optional<HiddenMessage> findByMessageIdAndUserId(Long messageId, Long userId);

    // Rimuovi nascondimento (rendi di nuovo visibile)
    void deleteByMessageIdAndUserId(Long messageId, Long userId);

    // Conta quanti utenti hanno nascosto un messaggio
    long countByMessageId(Long messageId);
}
