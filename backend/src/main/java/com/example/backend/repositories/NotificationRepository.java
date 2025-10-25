package com.example.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Notifiche per utente
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Notifiche non lette
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Conta notifiche non lette per tipo
    @Query("""
        SELECT n.type as type, COUNT(n) as count 
        FROM Notification n 
        WHERE n.user.id = :userId AND n.isRead = false 
        GROUP BY n.type
        """)
    List<Map<String, Object>> countUnreadByType(@Param("userId") Long userId);

    // Conta totale notifiche non lette
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Carica le ultime N notifiche con eager loading delle relazioni necessarie.
     * Questa query è ottimizzata per il dropdown delle notifiche nell'header.

     */
    @Query("""
        SELECT DISTINCT n FROM Notification n
        LEFT JOIN FETCH n.triggeredByUser
        WHERE n.user.id = :userId
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findRecentNotifications(@Param("userId") Long userId, Pageable pageable);

    /**
     * Carica notifiche con tutti i dettagli (post, commento, messaggio correlati).
     * Questa è una versione "pesante" che carica tutte le entità correlate.

     */
    @Query("""
        SELECT DISTINCT n FROM Notification n
        LEFT JOIN FETCH n.triggeredByUser
        LEFT JOIN FETCH n.relatedPost
        LEFT JOIN FETCH n.relatedComment
        LEFT JOIN FETCH n.relatedMessage
        WHERE n.id IN :ids
        """)
    List<Notification> findByIdInWithDetails(@Param("ids") List<Long> ids);

    // Segna come lette
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    // Segna singola notifica come letta
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    // Elimina vecchie notifiche
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    void deleteOldNotifications(@Param("threshold") LocalDateTime threshold);

    /**
     * Trova notifiche duplicate per evitare di creare notifiche ridondanti.

     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.user.id = :userId
        AND n.triggeredByUser.id = :triggeredByUserId
        AND n.type = :type
        AND n.createdAt > :since
        AND (
            (n.relatedPost.id = :relatedId AND :contentType = 'POST')
            OR (n.relatedComment.id = :relatedId AND :contentType = 'COMMENT')
            OR (n.relatedMessage.id = :relatedId AND :contentType = 'MESSAGE')
        )
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findDuplicateNotifications(
            @Param("userId") Long userId,
            @Param("triggeredByUserId") Long triggeredByUserId,
            @Param("type") com.example.backend.models.NotificationType type,
            @Param("relatedId") Long relatedId,
            @Param("contentType") String contentType,
            @Param("since") LocalDateTime since
    );
}