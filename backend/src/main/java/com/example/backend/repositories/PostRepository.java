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
    
    // Post con almeno N likes
    @Query("SELECT p FROM Post p WHERE p.likesCount >= :minLikes AND p.isDeletedByAuthor = false")
    List<Post> findPopularPosts(@Param("minLikes") Integer minLikes);
    
    // Aggiorna contatore likes (usato se non usi trigger)
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + :delta WHERE p.id = :postId")
    void updateLikesCount(@Param("postId") Long postId, @Param("delta") Integer delta);
    
    // Aggiorna contatore commenti (usato se non usi trigger)
    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + :delta WHERE p.id = :postId")
    void updateCommentsCount(@Param("postId") Long postId, @Param("delta") Integer delta);
}