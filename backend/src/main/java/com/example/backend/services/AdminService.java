package com.example.backend.services;

import com.example.backend.exception.InvalidInputException;
import com.example.backend.models.*;
import com.example.backend.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final DirectMessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final AdminAuditService auditService;

    private static final String TARGET_TYPE_USER = "USER";
    private static final String TARGET_TYPE_POST = "POST";
    private static final String TARGET_TYPE_COMMENT = "COMMENT";
    private static final String USER_NOT_FOUND_MESSAGE = "Utente non trovato";

    /**
     * Elimina un utente e tutti i suoi contenuti
     */
    @Transactional
    public void eliminaUtente(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} elimina utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));
            // Impedisce eliminazione admin
            if (target.getIsAdmin().booleanValue()) {
                throw new IllegalStateException("Non è possibile eliminare un account admin");
            }

            // Elimina tutti i contenuti dell'utente
            eliminaTuttiPost(target.getId());
            eliminaTuttiCommenti(target.getId());
            eliminaTuttiLike(target.getId());
            eliminaTuttiMessaggi(target.getId());
            eliminaTutteNotifiche(target.getId());

            // Elimina l'utente
            userRepository.delete(target);

            // Log audit
            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.ELIMINA_UTENTE,
                    "Eliminazione utente " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Utente {} eliminato completamente", target.getUsername());
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Disattiva account utente
     */
    @Transactional
    public void disattivaUtente(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} disattiva utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            if (target.getIsAdmin().booleanValue()) {
                throw new IllegalStateException("Non è possibile disattivare un account admin");
            }

            target.setIsActive(false);
            userRepository.save(target);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.DISATTIVA_UTENTE,
                    "Disattivazione utente " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Utente {} disattivato", target.getUsername());
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Riattiva account utente
     */
    @Transactional
    public void riattivaUtente(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} riattiva utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            target.setIsActive(true);
            userRepository.save(target);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.RIATTIVA_UTENTE,
                    "Riattivazione utente " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Utente {} riattivato", target.getUsername());
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Promuove utente ad admin
     */
    @Transactional
    public void promouviAdmin(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} promuove utente {} ad admin", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            if (target.getIsAdmin().booleanValue()) {
                throw new IllegalStateException("L'utente è già admin");
            }

            target.setIsAdmin(true);
            userRepository.save(target);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.PROMUOVI_ADMIN,
                    "Promozione a admin di " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Utente {} promosso ad admin", target.getUsername());
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Rimuove privilegi admin
     */
    @Transactional
    public void rimuoviAdmin(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} rimuove privilegi admin a utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            if (!target.getIsAdmin().booleanValue()) {
                throw new IllegalStateException("L'utente non è admin");
            }

            // Impedisce rimuovere privilegi a se stesso
            if (adminId.equals(targetUserId)) {
                throw new IllegalStateException("Non puoi rimuovere i tuoi stessi privilegi");
            }

            target.setIsAdmin(false);
            userRepository.save(target);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.RIMUOVI_ADMIN,
                    "Rimozione privilegi admin di " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Privilegi admin rimossi a {}", target.getUsername());
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Elimina un post specifico
     */
    @Transactional
    public void eliminaPost(Long adminId, Long postId, HttpServletRequest request) {
        log.info("Admin {} elimina post {}", adminId, postId);
        if (adminId != null && postId != null) {
            User admin = userRepository.getReferenceById(adminId);
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post non trovato"));

            // Elimina tutte le notifiche associate al post
            int notificationsDeleted = notificationRepository.deleteByRelatedPostId(postId);
            if (notificationsDeleted > 0) {
                log.debug("Eliminate {} notifiche associate al post {}", notificationsDeleted, postId);
            }

            // Elimina il post
            postRepository.delete(post);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.ELIMINA_POST,
                    "Eliminazione post ID " + postId + " di " + post.getUser().getUsername(),
                    TARGET_TYPE_POST,
                    postId,
                    post.getUser(),
                    request);

            log.info("Post {} eliminato", postId);
        } else {
            throw new InvalidInputException("ID admin e ID post sono richiesti");
        }

    }

    /**
     * Elimina un commento specifico
     */
    @Transactional
    public void eliminaCommento(Long adminId, Long commentId, HttpServletRequest request) {
        log.info("Admin {} elimina commento {}", adminId, commentId);
        if (adminId != null && commentId != null) {
            User admin = userRepository.getReferenceById(adminId);
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("Commento non trovato"));

            // Elimina il commento
            commentRepository.delete(comment);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.ELIMINA_COMMENTO,
                    "Eliminazione commento ID " + commentId + " di " + comment.getUser().getUsername(),
                    TARGET_TYPE_COMMENT,
                    commentId,
                    comment.getUser(),
                    request);

            log.info("Commento {} eliminato", commentId);
        } else {
            throw new InvalidInputException("ID admin e ID commento sono richiesti");
        }

    }

    /**
     * Elimina tutti i post di un utente
     */
    @Transactional
    public int eliminaTuttiPostUtente(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} elimina tutti i post dell'utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            int count = eliminaTuttiPost(targetUserId);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.ELIMINA_TUTTI_POST,
                    "Eliminazione di " + count + " post di " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Eliminati {} post dell'utente {}", count, target.getUsername());
            return count;
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Elimina tutti i commenti di un utente
     */
    @Transactional
    public int eliminaTuttiCommentiUtente(Long adminId, Long targetUserId, HttpServletRequest request) {
        log.info("Admin {} elimina tutti i commenti dell'utente {}", adminId, targetUserId);
        if (adminId != null && targetUserId != null) {
            User admin = userRepository.getReferenceById(adminId);
            User target = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new RuntimeException(USER_NOT_FOUND_MESSAGE));

            int count = eliminaTuttiCommenti(targetUserId);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.ELIMINA_TUTTI_COMMENTI,
                    "Eliminazione di " + count + " commenti di " + target.getUsername(),
                    TARGET_TYPE_USER,
                    targetUserId,
                    target,
                    request);

            log.info("Eliminati {} commenti dell'utente {}", count, target.getUsername());
            return count;
        } else {
            throw new InvalidInputException("ID admin e ID utente target sono richiesti");
        }

    }

    /**
     * Ottiene statistiche sistema
     */
    @Transactional
    public Map<String, Object> ottieniStatisticheSistema(Long adminId, HttpServletRequest request) {
        log.info("Admin {} visualizza statistiche sistema", adminId);
        if (adminId != null) {
            User admin = userRepository.getReferenceById(adminId);

            // Conta utenti
            long totaleUtenti = userRepository.count();
            long utentiAttivi = userRepository.findByIsActiveTrue().size();
            long utentiAdmin = userRepository.findAll().stream()
                    .filter(User::getIsAdmin)
                    .count();

            // Conta contenuti
            long totalePosts = postRepository.count();
            long totaleCommenti = commentRepository.count();
            long totaleLikes = likeRepository.count();
            long totaleMessaggi = messageRepository.count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("utenti", Map.of(
                    "totale", totaleUtenti,
                    "attivi", utentiAttivi,
                    "admin", utentiAdmin,
                    "disattivati", totaleUtenti - utentiAttivi));
            stats.put("contenuti", Map.of(
                    "posts", totalePosts,
                    "commenti", totaleCommenti,
                    "likes", totaleLikes,
                    "messaggi", totaleMessaggi));

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.VISUALIZZA_STATISTICHE,
                    "Visualizzazione statistiche sistema",
                    null,
                    null,
                    null,
                    request);

            return stats;
        } else {
            throw new InvalidInputException("ID admin è richiesto");
        }

    }

    /**
     * Ottiene audit log paginato
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLog> ottieniAuditLog(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Ottiene audit log per admin specifico
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLog> ottieniAuditLogAdmin(Long adminId, Pageable pageable) {
        return auditLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
    }

    /**
     * Ottiene audit log per azione specifica
     */
    @Transactional(readOnly = true)
    public Page<AdminAuditLog> ottieniAuditLogPerAzione(AzioneAdmin azione, Pageable pageable) {
        return auditLogRepository.findByAzioneOrderByCreatedAtDesc(azione, pageable);
    }

    /**
     * Pulizia database - elimina contenuti vecchi.
     *
     * ATOMICITÀ:
     * Questa operazione è @Transactional, quindi o viene completata interamente o
     * rollbackata.
     * I metodi di eliminazione restituiscono il numero di righe eliminate.
     *
     * @param adminId          L'ID dell'admin che esegue la pulizia
     * @param giorniVecchiezza Numero di giorni - i contenuti più vecchi verranno
     *                         eliminati
     * @param request          La richiesta HTTP per audit log
     * @return Mappa con i conteggi degli elementi eliminati
     */
    @Transactional
    public Map<String, Integer> puliziaDatabase(Long adminId, int giorniVecchiezza,
            HttpServletRequest request) {
        log.info("Admin {} avvia pulizia database (giorni: {})", adminId, giorniVecchiezza);
        if (adminId != null) {
            User admin = userRepository.getReferenceById(adminId);

            // Elimina notifiche vecchie (restituisce il numero di notifiche eliminate)
            LocalDateTime threshold = LocalDateTime.now().minusDays(giorniVecchiezza);
            int notificheEliminate = notificationRepository.deleteOldNotifications(threshold);

            // Elimina messaggi marcati come permanently deleted (restituisce il numero
            // eliminato)
            int messaggiEliminati = messageRepository.deletePermanentlyDeletedMessages();

            Map<String, Integer> risultati = new HashMap<>();
            risultati.put("notificheEliminate", notificheEliminate);
            risultati.put("messaggiEliminati", messaggiEliminati);

            auditService.logAzioneAdmin(
                    admin,
                    AzioneAdmin.PULIZIA_DATABASE,
                    String.format("Pulizia database - Eliminati %d notifiche e %d messaggi (contenuti > %d giorni)",
                            notificheEliminate, messaggiEliminati, giorniVecchiezza),
                    null,
                    null,
                    null,
                    request);

            log.info("Pulizia database completata - {} notifiche e {} messaggi eliminati",
                    notificheEliminate, messaggiEliminati);
            return risultati;
        } else {
            throw new InvalidInputException("ID admin è richiesto");
        }

    }

    // Metodi privati helper

    /**
     * Elimina tutti i post di un utente.
     * Questa è un'operazione atomica: elimina i post e i loro contenuti correlati.
     * I contatori non vengono aggiornati perché i post vengono eliminati
     * completamente.
     * 
     */
    private int eliminaTuttiPost(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);
        int count = posts.size();

        // L'eliminazione dei post eliminerà in cascade:
        // - tutti i commenti (via CascadeType.ALL)
        // - tutti i like (via CascadeType.ALL)

        postRepository.deleteAll(posts);

        log.debug("Eliminati {} post dell'utente {}", count, userId);
        return count;
    }

    /**
     * Elimina tutti i commenti di un utente.
     *
     * THREAD-SAFETY e ATOMICITÀ:
     * - Ottiene la lista dei post affetti prima dell'eliminazione
     * - Elimina tutti i commenti in batch
     * - Sincronizza atomicamente i contatori per ogni post affetto
     *
     *
     */
    private int eliminaTuttiCommenti(Long userId) {
        List<Comment> comments = commentRepository.findByUserId(userId);
        int count = comments.size();

        if (count == 0) {
            return 0;
        }

        // Ottieni lista dei post_id che verranno affetti
        List<Long> affectedPostIds = comments.stream()
                .map(c -> c.getPost().getId())
                .distinct()
                .toList();

        // Elimina tutti i commenti
        commentRepository.deleteAll(comments);

        // Sincronizza atomicamente i contatori per ogni post affetto
        for (Long postId : affectedPostIds) {
            postRepository.syncCommentsCount(postId);
        }

        log.debug("Eliminati {} commenti dell'utente {} da {} post",
                count, userId, affectedPostIds.size());
        return count;
    }

    /**
     * Elimina tutti i like di un utente.
     *
     * THREAD-SAFETY e ATOMICITÀ:
     * - Ottiene la lista dei post affetti prima dell'eliminazione
     * - Elimina tutti i like in una singola operazione bulk atomica
     * - Sincronizza atomicamente i contatori per ogni post affetto
     *
     * 
     */
    private void eliminaTuttiLike(Long userId) {
        // Ottieni la lista dei post_id che verranno affetti
        List<Long> affectedPostIds = likeRepository.findPostIdsByUserId(userId);

        if (affectedPostIds.isEmpty()) {
            return;
        }

        // Elimina tutti i like in una singola operazione bulk atomica
        int count = likeRepository.deleteAllByUserId(userId);

        // Sincronizza i contatori atomicamente per ogni post affetto
        for (Long postId : affectedPostIds) {
            postRepository.syncLikesCount(postId);
        }

        log.debug("Eliminati {} like dell'utente {} da {} post",
                count, userId, affectedPostIds.size());
    }

    /**
     * Elimina tutti i messaggi di un utente.
     * Include sia messaggi inviati che ricevuti.
     *
     * 
     */
    private void eliminaTuttiMessaggi(Long userId) {
        List<DirectMessage> messages = messageRepository.findAllByUserId(userId);
        int count = messages.size();
        messageRepository.deleteAll(messages);
        log.debug("Eliminati {} messaggi dell'utente {}", count, userId);
    }

    /**
     * Elimina tutte le notifiche di un utente.
     *
     * 
     */
    private void eliminaTutteNotifiche(Long userId) {
        notificationRepository.deleteByUserId(userId);
        log.debug("Eliminate tutte le notifiche dell'utente {}", userId);
    }
}