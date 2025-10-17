package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.models.HiddenPost;

import java.util.Optional;

@Repository
public interface HiddenPostRepository extends JpaRepository<HiddenPost, Long> {
    
    // Verifica se un post è nascosto per un utente
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    
    // Trova record specifico
    Optional<HiddenPost> findByPostIdAndUserId(Long postId, Long userId);
    
    // Rimuovi nascondimento (rendi di nuovo visibile)
    void deleteByPostIdAndUserId(Long postId, Long userId);
    
    // Conta quanti utenti hanno nascosto un post
    long countByPostId(Long postId);
}
