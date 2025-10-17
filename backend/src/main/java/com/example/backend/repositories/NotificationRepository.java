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
    
    // Segna come lette
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);
    
    // Segna singola notifica come letta
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.user.id = :userId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("userId") Long userId);
    
    // Elimina vecchie notifiche (per pulizia)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    void deleteOldNotifications(@Param("threshold") LocalDateTime threshold);
}

