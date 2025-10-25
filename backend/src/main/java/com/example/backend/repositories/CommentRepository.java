package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Commenti visibili per un post
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.isDeletedByAuthor = false
        AND NOT EXISTS (
            SELECT hc FROM HiddenComment hc 
            WHERE hc.comment.id = c.id AND hc.user.id = :userId
        )
        ORDER BY c.createdAt ASC
        """)
    List<Comment> findVisibleCommentsForPost(@Param("postId") Long postId, @Param("userId") Long userId);

    // Commenti principali
    @Query("""
        SELECT c FROM Comment c
        WHERE c.post.id = :postId
        AND c.parentComment IS NULL
        AND c.isDeletedByAuthor = false
        ORDER BY c.createdAt ASC
        """)
    List<Comment> findRootCommentsByPostId(@Param("postId") Long postId);

    // Risposte a un commento
    List<Comment> findByParentCommentIdAndIsDeletedByAuthorFalseOrderByCreatedAtAsc(Long parentCommentId);

    // Conta commenti per post
    long countByPostIdAndIsDeletedByAuthorFalse(Long postId);

    /**
     * Conta i commenti totali scritti da un utente.
     * Include sia commenti principali che risposte.
     * Utile per mostrare statistiche nel profilo utente.
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.isDeletedByAuthor = false")
    long countByUserId(@Param("userId") Long userId);

}