package com.example.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.DirectMessage;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    
    // Conversazione tra due utenti
    @Query("""
        SELECT dm FROM DirectMessage dm
        WHERE dm.isDeletedPermanently = false
        AND (
            (dm.sender.id = :user1Id AND dm.receiver.id = :user2Id AND dm.isDeletedBySender = false)
            OR
            (dm.sender.id = :user2Id AND dm.receiver.id = :user1Id AND dm.isDeletedByReceiver = false)
        )
        ORDER BY dm.createdAt ASC
        """)
    List<DirectMessage> findConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
    
    // Messaggi non letti per un utente
    @Query("""
        SELECT dm FROM DirectMessage dm
        WHERE dm.receiver.id = :userId
        AND dm.isRead = false
        AND dm.isDeletedPermanently = false
        AND dm.isDeletedByReceiver = false
        ORDER BY dm.createdAt DESC
        """)
    List<DirectMessage> findUnreadMessages(@Param("userId") Long userId);
    
    // Conta messaggi non letti
    @Query("""
        SELECT COUNT(dm) FROM DirectMessage dm
        WHERE dm.receiver.id = :userId
        AND dm.isRead = false
        AND dm.isDeletedPermanently = false
        AND dm.isDeletedByReceiver = false
        """)
    long countUnreadMessages(@Param("userId") Long userId);
    
    // Segna come letti
    @Modifying
    @Query("""
        UPDATE DirectMessage dm 
        SET dm.isRead = true 
        WHERE dm.receiver.id = :receiverId 
        AND dm.sender.id = :senderId
        AND dm.isRead = false
        """)
    void markMessagesAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);
    
    // Ultime conversazioni
    @Query("""
        SELECT dm FROM DirectMessage dm
        WHERE dm.id IN (
            SELECT MAX(dm2.id) FROM DirectMessage dm2
            WHERE (dm2.sender.id = :userId OR dm2.receiver.id = :userId)
            AND dm2.isDeletedPermanently = false
            GROUP BY CASE 
                WHEN dm2.sender.id = :userId THEN dm2.receiver.id 
                ELSE dm2.sender.id 
            END
        )
        ORDER BY dm.createdAt DESC
        """)
    Page<DirectMessage> findLatestConversations(@Param("userId") Long userId, Pageable pageable);
}
