package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.Like;
import com.example.backend.models.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    
    // Verifica se un utente ha messo like a un post
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    // Trova like specifico
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);
    
    // Rimuovi like
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    // Conta likes per post
    long countByPostId(Long postId);
    
    // Utenti che hanno messo like
    @Query("SELECT l.user FROM Like l WHERE l.post.id = :postId")
    List<User> findUsersWhoLikedPost(@Param("postId") Long postId);
}
