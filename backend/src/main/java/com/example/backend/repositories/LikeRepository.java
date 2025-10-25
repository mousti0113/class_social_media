package com.example.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Utenti che hanno messo like (lista semplice)
    @Query("SELECT l.user FROM Like l WHERE l.post.id = :postId")
    List<User> findUsersWhoLikedPost(@Param("postId") Long postId);

    /**
     *Query paginata per ottenere gli utenti che hanno messo like a un post.
     *
     * Ordinamento per data di creazione del like: i like più recenti appaiono per primi.
     */
    @Query("SELECT l.user FROM Like l WHERE l.post.id = :postId ORDER BY l.createdAt DESC")
    Page<User> findUsersWhoLikedPostPaginated(@Param("postId") Long postId, Pageable pageable);

    /**
     * Trova tutti i like di un post con i dettagli completi.
     * Utile quando per mostrare non solo chi ha messo like, ma anche quando l'ha fatto.
     *  Con ordinamento per data.
     */
    @Query("SELECT l FROM Like l WHERE l.post.id = :postId ORDER BY l.createdAt DESC")
    Page<Like> findLikesByPostIdPaginated(@Param("postId") Long postId, Pageable pageable);


}