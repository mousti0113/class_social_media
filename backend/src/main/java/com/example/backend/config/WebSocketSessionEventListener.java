package com.example.backend.config;

import com.example.backend.models.User;
import com.example.backend.models.UserSession;
import com.example.backend.repositories.UserRepository;
import com.example.backend.repositories.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Event Listener per tracciare connessioni e disconnessioni WebSocket.
 * <p>
 * Gestisce gli eventi del ciclo di vita delle sessioni WebSocket:
 * - CONNECT: Quando un client si connette
 * - SUBSCRIBE: Quando un client si sottoscrive a un topic/queue
 * - UNSUBSCRIBE: Quando un client si disiscrive
 * - DISCONNECT: Quando un client si disconnette
 * <p>
 * Mantiene una mappa in-memory degli utenti connessi per:
 * - Mostrare lo stato "online" nel frontend
 * - Statistiche di sistema
 * - Debug e monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionEventListener {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    // Mappa sessionId -> username per tracciare utenti connessi (per performance)
    private final Map<String, String> activeUsers = new ConcurrentHashMap<>();

    /**
     * Gestisce l'evento di connessione WebSocket.
     * <p>
     * Viene invocato quando un client stabilisce con successo
     * una connessione STOMP over WebSocket.
     * <p>
     * Aggiorna anche il database creando/aggiornando la UserSession.
     *
     * @param event L'evento di connessione
     */
    @EventListener
    @Transactional
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            activeUsers.put(sessionId, username);

            log.info("WebSocket Connected - User: {}, SessionId: {}", username, sessionId);
            log.debug("Utenti attualmente connessi: {}", activeUsers.size());

            // Aggiorna o crea UserSession nel database
            try {
                Optional<UserSession> existingSession = userSessionRepository.findBySessionId(sessionId);

                if (existingSession.isPresent()) {
                    // Sessione già esistente, aggiorna
                    UserSession session = existingSession.get();
                    session.setIsOnline(true);
                    session.setLastActivity(LocalDateTime.now());
                    userSessionRepository.save(session);
                    log.debug("UserSession aggiornata per {}", username);
                } else {
                    // Nuova sessione, viene creata
                    User userEntity = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Utente non trovato: " + username));

                    UserSession newSession = UserSession.builder()
                            .user(userEntity)
                            .sessionId(sessionId)
                            .isOnline(true)
                            .lastActivity(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .build();

                    userSessionRepository.save(newSession);
                    log.info("Nuova UserSession creata per {} - SessionId: {}", username, sessionId);
                }
            } catch (Exception e) {
                log.error("Errore aggiornamento UserSession per {}: {}", username, e.getMessage());
            }
        } else {
            log.warn("WebSocket Connected senza autenticazione - SessionId: {}", sessionId);
        }
    }

    /**
     * Gestisce l'evento di disconnessione WebSocket.
     * <p>
     * Viene invocato quando un client chiude la connessione
     * (volontariamente o per timeout/errore).
     * <p>
     * Rimuove l'utente dalla mappa in-memory e marca la sessione
     * come offline nel database.
     *
     * @param event L'evento di disconnessione
     */
    @EventListener
    @Transactional
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = activeUsers.remove(sessionId);

        if (username != null) {
            log.info("WebSocket Disconnected - User: {}, SessionId: {}", username, sessionId);
            log.debug("Utenti rimanenti connessi: {}", activeUsers.size());

            // Aggiorna UserSession nel database
            try {
                Optional<UserSession> session = userSessionRepository.findBySessionId(sessionId);

                if (session.isPresent()) {
                    UserSession userSession = session.get();
                    userSession.setIsOnline(false);
                    userSession.setLastActivity(LocalDateTime.now());
                    userSessionRepository.save(userSession);
                    log.info("UserSession marcata offline per {} - SessionId: {}", username, sessionId);
                } else {
                    log.warn("UserSession non trovata per sessionId: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("Errore aggiornamento UserSession offline per {}: {}", username, e.getMessage());
            }
        } else {
            log.debug("WebSocket Disconnected - SessionId: {}", sessionId);
        }
    }

    /**
     * Gestisce l'evento di sottoscrizione a un topic/queue.
     * <p>
     * Viene invocato quando un client si sottoscrive a una destinazione
     * (es: /user/queue/notifications).
     * <p>
     * Utile per debug e monitoring.
     *
     * @param event L'evento di sottoscrizione
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String destination = headerAccessor.getDestination();

        if (user != null && destination != null) {
            log.debug("User {} subscribed to: {}", user.getName(), destination);
        }
    }

    /**
     * Gestisce l'evento di disiscrizione da un topic/queue.
     * <p>
     * Viene invocato quando un client si disiscreve da una destinazione.
     *
     * @param event L'evento di disiscrizione
     */
    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            log.debug("User {} unsubscribed from a destination", user.getName());
        }
    }

    /**
     * Verifica se un utente è attualmente connesso via WebSocket.
     * <p>
     * Utile per mostrare lo stato "online" nel frontend.
     *
     * @param username L'username da verificare
     * @return true se l'utente è connesso, false altrimenti
     */
    public boolean isUserConnected(String username) {
        return activeUsers.containsValue(username);
    }

    /**
     * Ottiene il numero di utenti attualmente connessi.
     *
     * @return Il numero di connessioni WebSocket attive
     */
    public int getActiveConnectionsCount() {
        return activeUsers.size();
    }

    /**
     * Ottiene la lista degli username degli utenti connessi.
     *
     * @return Set di username connessi
     */
    public Set<String> getActiveUsernames() {
        return new HashSet<>(activeUsers.values());
    }
}
