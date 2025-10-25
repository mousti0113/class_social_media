package com.example.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.Post;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Post visibili per un utente (non cancellati e non nascosti)
    @Query("""
        SELECT p FROM Post p
        WHERE p.isDeletedByAuthor = false
        AND NOT EXISTS (
            SELECT hp FROM HiddenPost hp 
            WHERE hp.post.id = p.id AND hp.user.id = :userId
        )
        ORDER BY p.createdAt DESC
        """)
    Page<Post> findVisiblePostsForUser(@Param("userId") Long userId, Pageable pageable);

    // Post di un utente specifico
    Page<Post> findByUserIdAndIsDeletedByAuthorFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);



    /**
     * Carica un singolo post con tutti i dettagli necessari in una sola query.
     * Usa JOIN FETCH per caricare eagerly l'autore del post, i commenti principali ,
     * gli autori dei commenti e le risposte ai commenti. Questo evita il problema N+1.
     *
     * DISTINCT è necessario perché JOIN FETCH su collezioni può creare duplicati nel result set.
     * Hibernate rimuoverà automaticamente i duplicati ma senza DISTINCT la query SQL potrebbe
     * restituire più righe per lo stesso post.

     */
    @Query("""
        SELECT DISTINCT p FROM Post p
        LEFT JOIN FETCH p.user
        LEFT JOIN FETCH p.comments c
        LEFT JOIN FETCH c.user
        LEFT JOIN FETCH c.childComments cc
        LEFT JOIN FETCH cc.user
        WHERE p.id = :postId
        AND p.isDeletedByAuthor = false
        """)
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);

    /**
     * Ricerca full-text nei post. Cerca sia nel contenuto del post che nello username dell'autore.
     * La ricerca è case-insensitive grazie a LOWER() e utilizza LIKE con % per matching parziale.

     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.isDeletedByAuthor = false
        AND (
            LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(p.user.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(p.user.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        ORDER BY p.createdAt DESC
        """)
    Page<Post> searchPosts(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Conta il numero totale di post pubblicati da un utente.
     * Utile per mostrare statistiche nel profilo utente.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.isDeletedByAuthor = false")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Trova i post più recenti di un utente specifico, visibili per l'utente che sta guardando.
     * Questa query è diversa da findByUserIdAndIsDeletedByAuthorFalseOrderByCreatedAtDesc
     * perché filtra anche i post che l'utente corrente ha nascosto.
     */
    @Query("""
        SELECT p FROM Post p
        WHERE p.user.id = :authorId
        AND p.isDeletedByAuthor = false
        AND NOT EXISTS (
            SELECT hp FROM HiddenPost hp 
            WHERE hp.post.id = p.id AND hp.user.id = :currentUserId
        )
        ORDER BY p.createdAt DESC
        """)
    Page<Post> findVisiblePostsByUser(
            @Param("authorId") Long authorId,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    // Aggiorna contatore likes
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + :delta WHERE p.id = :postId")
    void updateLikesCount(@Param("postId") Long postId, @Param("delta") Integer delta);

    // Aggiorna contatore commenti
    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + :delta WHERE p.id = :postId")
    void updateCommentsCount(@Param("postId") Long postId, @Param("delta") Integer delta);
}