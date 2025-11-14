package com.example.backend.repositories;


import com.example.backend.exception.LimitExceededException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Conta tutti gli utenti (17 max, incluso admin)
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

    /**
     * Ricerca utenti per username (autocomplete menzioni).
     * Cerca solo utenti il cui username inizia con il prefisso fornito.
     * Utile per l'autocomplete quando si digita @ seguito da lettere.
     * <p>
     * PERFORMANCE: Usa Pageable per limitare i risultati direttamente nel DB
     * invece di caricare tutti gli utenti e limitare in Java.
     *
     * @param prefix Il prefisso da cercare
     * @param pageable Parametri di paginazione (usa PageRequest.of(0, 10) per primi 10)
     * @return Lista di utenti che matchano il prefisso
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true AND LOWER(u.username) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY u.username")
    List<User> findByUsernameStartingWith(@Param("prefix") String prefix, Pageable pageable);

    /**
     * Trova utenti per una lista di username.
     * Utile per processare le menzioni: dato un set di @username,
     * recupera tutti gli User corrispondenti.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.username) IN :usernames AND u.isActive = true")
    List<User> findByUsernameIn(@Param("usernames") java.util.Set<String> usernames);

    /**
     * Ricerca avanzata di utenti per username o nome completo.
     * La ricerca è case-insensitive e cerca in entrambi i campi.
     * Restituisce risultati paginati per gestire liste di utenti lunghe.
     *
     *
     * Questa query cerca la stringa ovunque nel campo (inizio, mezzo, fine).
     * Utile per la barra di ricerca generale dell'applicazione.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND (
            LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        )
        ORDER BY u.username ASC
        """)
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Trova tutti gli utenti attivi con paginazione.
     * Ordinati alfabeticamente per username.
     * Utile per mostrare la lista completa degli studenti della classe.
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.username ASC")
    Page<User> findAllActiveUsers(Pageable pageable);

    /**
     * Conta i post creati da un utente specifico.
     * Utile per mostrare statistiche nel profilo.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.user.id = :userId AND p.isDeletedByAuthor = false")
    long countPostsByUserId(@Param("userId") Long userId);

    /**
     * Conta i commenti scritti da un utente specifico.
     * Include sia commenti principali che risposte.
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.isDeletedByAuthor = false")
    long countCommentsByUserId(@Param("userId") Long userId);

    /**
     * Conta quanti like totali hanno ricevuto i post di un utente.
     * Questa è una metrica importante per il profilo: mostra quanto è apprezzato il contenuto dell'utente.
     */
    @Query("SELECT SUM(p.likesCount) FROM Post p WHERE p.user.id = :userId AND p.isDeletedByAuthor = false")
    Long countTotalLikesReceivedByUserId(@Param("userId") Long userId);


    // Metodo default per validare limite studenti
    default void validateStudentLimit() {
        long count = count();
        if (count >= 17) {
            throw new LimitExceededException("Limite massimo di 17 studenti raggiunto");
        }
    }
}