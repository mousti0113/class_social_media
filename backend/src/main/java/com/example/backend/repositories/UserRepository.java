package com.example.backend.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.models.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    //  Conta tutti gli utenti (17 max, incluso admin)
    long count();



    // Trova l'admin
    Optional<User> findByIsAdminTrue();
    // Trova utenti attivi
    List<User> findByIsActiveTrue();
    
    // Trova utenti online
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.sessions s
        WHERE s.isOnline = true 
        AND s.lastActivity > :threshold
        """)
    List<User> findOnlineUsers(@Param("threshold") LocalDateTime threshold);
    
    // Ricerca utenti per username (autocomplete menzioni)
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<User> findByUsernameStartingWith(@Param("prefix") String prefix);
}

