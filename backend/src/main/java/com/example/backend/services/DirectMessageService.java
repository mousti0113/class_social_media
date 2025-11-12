package com.example.backend.services;

import com.example.backend.dtos.request.InviaMessaggioRequestDTO;
import com.example.backend.dtos.response.ConversationResponseDTO;
import com.example.backend.dtos.response.MessageResponseDTO;
import com.example.backend.exception.InvalidInputException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.mappers.MessageMapper;
import com.example.backend.models.DirectMessage;
import com.example.backend.models.User;
import com.example.backend.repositories.DirectMessageRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectMessageService {

    private final DirectMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;

    private static final String ENTITY_MESSAGE = "Messaggio";
    private static final String ENTITY_USER = "Utente";
    private static final String FIELD_ID = "id";

    /**
     * Invia un messaggio diretto a un altro utente
     */
    @Transactional
    public MessageResponseDTO inviaMessaggio(Long senderId, InviaMessaggioRequestDTO request) {
        log.info("Invio messaggio - Da utente: {} a utente: {}", senderId, request.getDestinatarioId());

        // Verifica che non stia mandando messaggi a se stesso
        if (senderId.equals(request.getDestinatarioId())) {
            throw new InvalidInputException("Non puoi inviare messaggi a te stesso");
        }

        // Carica mittente e destinatario
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_USER, FIELD_ID, senderId));

        User receiver = userRepository.findById(request.getDestinatarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        ENTITY_USER, FIELD_ID, request.getDestinatarioId()));

        // Crea il messaggio
        DirectMessage message = DirectMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContenuto())
                .isRead(false)
                .isDeletedBySender(false)
                .isDeletedByReceiver(false)
                .isDeletedPermanently(false)
                .build();

        message = messageRepository.save(message);
        log.info("Messaggio inviato - ID: {}", message.getId());



        // Crea notifica per il destinatario
        notificationService.creaNotificaMessaggio(receiver.getId(), sender.getId(), message.getId());

        return messageMapper.toMessaggioResponseDTO(message);
    }

    /**
     * Ottiene la conversazione tra due utenti
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> ottieniConversazione(Long userId, Long altroUtenteId) {
        log.debug("Caricamento conversazione tra utente {} e utente {}", userId, altroUtenteId);

        // Verifica che entrambi gli utenti esistano
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException(ENTITY_USER, FIELD_ID, userId);
        }
        if (!userRepository.existsById(altroUtenteId)) {
            throw new ResourceNotFoundException(ENTITY_USER, FIELD_ID, altroUtenteId);
        }

        // Carica i messaggi della conversazione
        List<DirectMessage> messages = messageRepository.findConversation(userId, altroUtenteId);

        log.debug("Trovati {} messaggi nella conversazione", messages.size());

        return messages.stream()
                .map(messageMapper::toMessaggioResponseDTO)
                .toList();
    }

    /**
     * Ottiene tutte le conversazioni dell'utente con preview ultimo messaggio
     */
    @Transactional(readOnly = true)
    public Page<ConversationResponseDTO> ottieniConversazioni(Long userId, Pageable pageable) {
        log.debug("Caricamento conversazioni per utente {}", userId);

        // Ottiene gli ultimi messaggi di ogni conversazione
        Page<DirectMessage> lastMessages = messageRepository.findLatestConversations(userId, pageable);

        // Per ogni ultimo messaggio, crea un DTO conversazione
        return lastMessages.map(lastMessage -> {
            // Identifica l'altro utente nella conversazione
            User altroUtente = lastMessage.getSender().getId().equals(userId)
                    ? lastMessage.getReceiver()
                    : lastMessage.getSender();

            // Conta messaggi non letti da questo utente
            int unreadCount = (int) messageRepository.countUnreadMessagesBySender(
                    userId, altroUtente.getId());

            return messageMapper.toConversazioneResponseDTO(
                    altroUtente, lastMessage, unreadCount);
        });
    }

    /**
     * Marca tutti i messaggi di una conversazione come letti
     */
    @Transactional
    public void marcaMessaggiComeLetti(Long receiverId, Long senderId) {
        log.debug("Marca messaggi come letti - Ricevente: {}, Mittente: {}", receiverId, senderId);

        messageRepository.markMessagesAsRead(receiverId, senderId);

        log.info("Messaggi marcati come letti");
    }

    /**
     * Conta tutti i messaggi non letti dell'utente
     */
    @Transactional(readOnly = true)
    public long contaMessaggiNonLetti(Long userId) {
        return messageRepository.countUnreadMessages(userId);
    }

    /**
     * Conta messaggi non letti da un utente specifico
     */
    @Transactional(readOnly = true)
    public long contaMessaggiNonLettiDaUtente(Long receiverId, Long senderId) {
        return messageRepository.countUnreadMessagesBySender(receiverId, senderId);
    }

    /**
     * Elimina un messaggio (soft delete)
     */
    @Transactional
    public void eliminaMessaggio(Long messageId, Long userId) {
        log.info("Eliminazione messaggio {} da utente {}", messageId, userId);

        DirectMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_MESSAGE, FIELD_ID, messageId));

        // Verifica che l'utente sia coinvolto nel messaggio
        if (!message.getSender().getId().equals(userId)
                && !message.getReceiver().getId().equals(userId)) {
            throw new UnauthorizedException("Non hai i permessi per eliminare questo messaggio");
        }

        // Soft delete in base al ruolo dell'utente
        if (message.getSender().getId().equals(userId)) {
            message.setDeletedBySender(true);
        } else {
            message.setDeletedByReceiver(true);
        }

        // Se entrambi hanno eliminato, elimina permanentemente
        if (message.isDeletedBySender() && message.isDeletedByReceiver()) {
            message.setDeletedPermanently(true);


        }

        messageRepository.save(message);
        log.info("Messaggio {} eliminato", messageId);
    }

    /**
     * Elimina intera conversazione con un utente
     */
    @Transactional
    public int eliminaConversazione(Long userId, Long altroUtenteId) {
        log.info("Eliminazione conversazione tra utente {} e utente {}", userId, altroUtenteId);

        // Usa la query ottimizzata che carica tutti i messaggi senza filtri
        List<DirectMessage> messages = messageRepository
                .findAllConversationMessages(userId, altroUtenteId);

        int count = 0;
        for (DirectMessage message : messages) {
            // Marca come eliminato dal lato dell'utente corrente
            if (message.getSender().getId().equals(userId)) {
                message.setDeletedBySender(true);
            } else {
                message.setDeletedByReceiver(true);
            }

            // Se entrambi hanno eliminato, elimina permanentemente
            if (message.isDeletedBySender() && message.isDeletedByReceiver()) {
                message.setDeletedPermanently(true);


            }

            count++;
        }

        // Salva tutti in batch
        messageRepository.saveAll(messages);

        log.info("Eliminati {} messaggi dalla conversazione", count);
        return count;
    }

    /**
     * Cerca messaggi per contenuto
     */
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> cercaMessaggi(Long userId, String searchTerm) {
        log.debug("Ricerca messaggi per utente {} - Termine: {}", userId, searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new InvalidInputException("Il termine di ricerca non può essere vuoto");
        }

        // Usa la query ottimizzata con LIKE
        List<DirectMessage> messages = messageRepository
                .searchMessagesByContent(userId, searchTerm.trim());

        log.debug("Trovati {} messaggi con termine '{}'", messages.size(), searchTerm);

        return messages.stream()
                .map(messageMapper::toMessaggioResponseDTO)
                .toList();
    }



    /**
     * Elimina tutti i messaggi di un utente (per cancellazione account)
     */
    @Transactional
    public int eliminaTuttiMessaggiUtente(Long userId) {
        log.info("Eliminazione tutti i messaggi dell'utente {}", userId);

        // Usa query ottimizzata
        List<DirectMessage> messages = messageRepository.findAllByUserId(userId);

        // Marca tutti come eliminati permanentemente
        for (DirectMessage msg : messages) {
            msg.setDeletedPermanently(true);
        }

        messageRepository.saveAll(messages);

        log.info("Eliminati {} messaggi dell'utente {}", messages.size(), userId);
        return messages.size();
    }

    /**
     * Pulisce messaggi eliminati permanentemente (scheduled job)
     */
    @Transactional
    public void pulisciMessaggiEliminatiPermanentemente() {
        log.info("Pulizia messaggi eliminati permanentemente");

        messageRepository.deletePermanentlyDeletedMessages();

        log.info("Messaggi eliminati permanentemente ripuliti dal database");
    }

    /**
     * Pulisce messaggi vecchi e letti (scheduled job)
     */
    @Transactional
    public int pulisciMessaggiVecchi(int giorni) {
        log.info("Pulizia messaggi più vecchi di {} giorni", giorni);

        LocalDateTime threshold = LocalDateTime.now().minusDays(giorni);
        List<DirectMessage> oldMessages = messageRepository.findOldReadMessages(threshold);

        // Marca come eliminati permanentemente
        for (DirectMessage msg : oldMessages) {
            msg.setDeletedPermanently(true);
        }

        messageRepository.saveAll(oldMessages);

        log.info("Marcati {} messaggi vecchi per eliminazione permanente", oldMessages.size());
        return oldMessages.size();
    }
}