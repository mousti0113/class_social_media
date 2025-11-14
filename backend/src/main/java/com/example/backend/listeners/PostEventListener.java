package com.example.backend.listeners;

import com.example.backend.events.PostCreatedEvent;
import com.example.backend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener per gli eventi relativi ai post.
 * <p>
 * Questo componente ascolta gli eventi del dominio e reagisce in modo asincrono,
 * permettendo di disaccoppiare le operazioni principali (es. creazione post)
 * dalle operazioni secondarie (es. invio notifiche).
 * <p>
 * I listener sono eseguiti in thread separati grazie a @Async,
 * quindi non bloccano mai la transazione originale.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostEventListener {

    private final NotificationService notificationService;

    /**
     * Gestisce l'evento di creazione di un nuovo post.
     * <p>
     * Quando viene pubblicato un nuovo post, questo listener:
     * 1. Viene eseguito in un thread separato (grazie a @Async)
     * 2. Invoca il servizio di notifiche per notificare tutti gli utenti
     * <p>
     * Questo approccio garantisce che:
     * - La transazione di creazione post non venga bloccata
     * - Eventuali errori nelle notifiche non facciano fallire la creazione del post
     * - Il sistema sia più scalabile e performante
     *
     * @param event L'evento contenente i dati del post creato
     */
    @Async
    @EventListener
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("Evento PostCreatedEvent ricevuto - Autore: {}, Post: {}",
                event.getAuthorId(), event.getPostId());

        try {
            // Chiama il servizio per creare le notifiche
            notificationService.creaNotificheNuovoPost(event.getAuthorId(), event.getPostId());

            log.info("Notifiche per nuovo post processate con successo - Post ID: {}",
                    event.getPostId());
        } catch (Exception e) {
            // Log dell'errore ma non propaghiamo l'eccezione
            // Il post è già stato creato, le notifiche sono "best effort"
            log.error("Errore durante il processing delle notifiche per post ID: {} - Errore: {}",
                    event.getPostId(), e.getMessage(), e);
        }
    }
}
