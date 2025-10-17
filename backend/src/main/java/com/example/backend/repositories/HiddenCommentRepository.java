package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.models.HiddenComment;

import java.util.Optional;

@Repository
public interface HiddenCommentRepository extends JpaRepository<HiddenComment, Long> {
    
    // Verifica se un commento è nascosto per un utente
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);
    
    // Trova record specifico
    Optional<HiddenComment> findByCommentIdAndUserId(Long commentId, Long userId);
    
    // Rimuovi nascondimento
    void deleteByCommentIdAndUserId(Long commentId, Long userId);
    
    // Conta quanti utenti hanno nascosto un commento
    long countByCommentId(Long commentId);
}
