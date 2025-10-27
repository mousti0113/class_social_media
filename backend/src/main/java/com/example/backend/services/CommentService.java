package com.example.backend.services;

import com.example.backend.dtos.request.CreaCommentoRequestDTO;
import com.example.backend.dtos.response.CommentResponseDTO;
import com.example.backend.exception.InvalidInputException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.mappers.CommentMapper;
import com.example.backend.models.Comment;
import com.example.backend.models.HiddenComment;
import com.example.backend.models.Post;
import com.example.backend.models.User;
import com.example.backend.repositories.CommentRepository;
import com.example.backend.repositories.HiddenCommentRepository;
import com.example.backend.repositories.PostRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service per la gestione dei commenti ai post.
 *
 * Gestisce una struttura gerarchica di commenti dove:
 * - Commenti principali: hanno parentComment = null
 * - Risposte: hanno parentComment != null e puntano al commento padre
 *
 * La profondità massima è di due livelli:
 * Post → Commento → Risposta; non è possibile rispondere a una risposta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HiddenCommentRepository hiddenCommentRepository;
    private final CommentMapper commentMapper;

    private static final String ENTITY_COMMENT = "Commento";
    private static final String ENTITY_POST = "Post";
    private static final String ENTITY_USER = "Utente";
    private static final String FIELD_ID = "id";
    private static final int MAX_COMMENT_DEPTH = 2;

    /**
     * Crea un nuovo commento o risposta a un post.
     *
     * Questo metodo gestisce sia i commenti principali che le risposte.
     * - Se parentCommentId è null: crea un commento principale al post
     * - Se parentCommentId è presente: crea una risposta a quel commento
     *
     * Validazioni:
     * - Il post deve esistere e non essere cancellato
     * - Se è una risposta, il commento padre deve esistere e appartenere allo stesso post
     * - Non è possibile rispondere a una risposta.
     *
     * Dopo aver creato il commento, aggiorna automaticamente il contatore commentsCount
     * del post usando il repository.
     *
     * @param postId L'ID del post a cui commentare
     * @param userId L'ID dell'utente che sta commentando
     * @param request Il DTO con il contenuto del commento e l'eventuale parentCommentId
     * @return CommentResponseDTO con i dati del commento appena creato
     * @throws ResourceNotFoundException se post, utente o commento padre non esistono
     * @throws InvalidInputException se si tenta di rispondere a una risposta (depth > 2)
     */
    @Transactional
    public CommentResponseDTO creaCommento(Long postId, Long userId, CreaCommentoRequestDTO request) {
        log.info("Creazione commento per post ID: {} da utente ID: {}", postId, userId);

        // Carica il post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_POST, FIELD_ID, postId));

        // Verifica che il post non sia cancellato
        if (post.getIsDeletedByAuthor()) {
            log.warn("Tentativo di commentare post cancellato - Post ID: {}", postId);
            throw new ResourceNotFoundException(ENTITY_POST, FIELD_ID, postId);
        }

        // Carica l'utente
        User autore = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_USER, FIELD_ID, userId));

        // Gestisce il commento padre se presente (per le risposte)
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            ENTITY_COMMENT, FIELD_ID, request.getParentCommentId()));

            // Verifica che il commento padre appartenga allo stesso post
           
            if (!parentComment.getPost().getId().equals(postId)) {
                log.warn("Tentativo di rispondere a commento di post diverso - Commento ID: {}, Post ID: {}",
                        request.getParentCommentId(), postId);
                throw new InvalidInputException(
                        "Il commento a cui stai rispondendo non appartiene a questo post");
            }

            // Verifica che il commento padre non sia già una risposta
            
            if (parentComment.getParentComment() != null) {
                log.warn("Tentativo di rispondere a una risposta - Commento ID: {}",
                        request.getParentCommentId());
                throw new InvalidInputException(
                        "Non è possibile rispondere a una risposta. Puoi rispondere solo ai commenti principali.");
            }

            // Verifica che il commento padre non sia cancellato
            if (parentComment.getIsDeletedByAuthor()) {
                log.warn("Tentativo di rispondere a commento cancellato - Commento ID: {}",
                        request.getParentCommentId());
                throw new ResourceNotFoundException(ENTITY_COMMENT, FIELD_ID, request.getParentCommentId());
            }
        }

        // Crea l'entità Comment
        Comment comment = Comment.builder()
                .post(post)
                .user(autore)
                .parentComment(parentComment)
                .content(request.getContenuto())
                .isDeletedByAuthor(false)
                .build();

        // Salva il commento
        comment = commentRepository.save(comment);

        // Aggiorna il contatore dei commenti nel post
        // Incrementa di 1 il contatore
        postRepository.updateCommentsCount(postId, 1);

        log.info("Commento creato con successo - ID: {} per post ID: {} da utente: {}",
                comment.getId(), postId, autore.getUsername());

        // Converti in DTO e restituisci
        return commentMapper.toCommentoResponseDTO(comment);
    }

    /**
     * Modifica un commento esistente.
     *
     * Permette all'autore di modificare il contenuto di un commento.
     * Solo l'autore del commento può modificarlo.
     * Non è possibile modificare commenti cancellati.
     *
     * La modifica aggiorna automaticamente il campo updatedAt grazie a @LastModifiedDate.
     * Questo permette di mostrare nell'interfaccia "Modificato" se updatedAt > createdAt.
     *
     * @param commentId L'ID del commento da modificare
     * @param userId L'ID dell'utente che richiede la modifica
     * @param nuovoContenuto Il nuovo contenuto del commento
     * @return CommentResponseDTO con i dati aggiornati
     * @throws ResourceNotFoundException se il commento non esiste o è cancellato
     * @throws UnauthorizedException se l'utente non è l'autore
     */
    @Transactional
    public CommentResponseDTO modificaCommento(Long commentId, Long userId, String nuovoContenuto) {
        log.info("Modifica commento ID: {} da utente ID: {}", commentId, userId);

        // Carica il commento
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_COMMENT, FIELD_ID, commentId));

        // Verifica che l'utente sia l'autore
        if (!comment.getUser().getId().equals(userId)) {
            log.warn("Tentativo di modifica commento non autorizzato - Commento ID: {}, Utente ID: {}",
                    commentId, userId);
            throw new UnauthorizedException("Non hai i permessi per modificare questo commento");
        }

        // Verifica che il commento non sia cancellato
        if (comment.getIsDeletedByAuthor()) {
            log.warn("Tentativo di modificare commento cancellato - Commento ID: {}", commentId);
            throw new ResourceNotFoundException(ENTITY_COMMENT, FIELD_ID, commentId);
        }

        // Valida il nuovo contenuto
        if (nuovoContenuto == null || nuovoContenuto.trim().isEmpty()) {
            throw new InvalidInputException("Il contenuto del commento non può essere vuoto");
        }

        // Aggiorna il contenuto
        comment.setContent(nuovoContenuto);

        // Salva 
        comment = commentRepository.save(comment);

        log.info("Commento modificato con successo - ID: {}", commentId);

        return commentMapper.toCommentoResponseDTO(comment);
    }

    /**
     * Elimina un commento.
     *
     * Come per i post, uso il soft delete: il commento non viene eliminato fisicamente
     * ma viene marcato come cancellato con isDeletedByAuthor = true.
     *
     * Comportamento:
     * - Se il commento ha risposte, queste rimangono visibili
     * - Il commento viene mostrato come "[Commento eliminato]" nell'interfaccia
     * - Il contatore commentsCount del post viene decrementato
     *
     *Faccio questo perché permette di mantenere la struttura della conversazione. Se elimino fisicamente
     * un commento con risposte, le risposte perderebbero contesto.
     *
     * @param commentId L'ID del commento da eliminare
     * @param userId L'ID dell'utente che richiede l'eliminazione
     * @throws ResourceNotFoundException se il commento non esiste
     * @throws UnauthorizedException se l'utente non è l'autore
     */
    @Transactional
    public void eliminaCommento(Long commentId, Long userId) {
        log.info("Eliminazione commento ID: {} da utente ID: {}", commentId, userId);

        // Carica il commento
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY_COMMENT, FIELD_ID, commentId));

        // Verifica che l'utente sia l'autore
      
        if (!comment.getUser().getId().equals(userId)) {
            log.warn("Tentativo di eliminazione commento non autorizzato - Commento ID: {}, Utente ID: {}",
                    commentId, userId);
            throw new UnauthorizedException("Non hai i permessi per eliminare questo commento");
        }

        // Soft delete
        comment.setIsDeletedByAuthor(true);
        commentRepository.save(comment);

        // Decrementa il contatore dei commenti nel post
        postRepository.updateCommentsCount(comment.getPost().getId(), -1);

        log.info("Commento eliminato con successo (soft delete) - ID: {}", commentId);
    }

    /**
     * Ottiene tutti i commenti di un post con struttura gerarchica.
     *
     * Questo metodo restituisce i commenti principali, e ogni commento include
     * le sue risposte nella proprietà "risposte" del DTO.
     *
     * La query nel repository filtra automaticamente:
     * - Commenti cancellati (isDeletedByAuthor = true)
     * - Commenti nascosti dall'utente corrente
     *
     * La struttura gerarchica viene costruita dal CommentMapper che ricorsivamente
     * mappa i childComments.
     *
     *
     *
     * @param postId L'ID del post
     * @param userId L'ID dell'utente che sta visualizzando 
     * @return Lista di CommentResponseDTO con struttura gerarchica
     * @throws ResourceNotFoundException se il post non esiste
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> ottieniCommentiPost(Long postId, Long userId) {
        log.debug("Caricamento commenti per post ID: {} e utente ID: {}", postId, userId);

        // Verifica che il post esista
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException(ENTITY_POST, FIELD_ID, postId);
        }

        // Carica solo i commenti principali 
        // Le risposte verranno caricate dal mapper tramite la relazione childComments
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);

        // Filtra manualmente i commenti nascosti dall'utente
       
        List<Comment> visibleRootComments = rootComments.stream()
                .filter(comment -> !comment.isHiddenForUser(userId))
                .collect(Collectors.toList());

        log.debug("Trovati {} commenti principali per post ID: {}", visibleRootComments.size(), postId);

        // Converte in DTO con struttura gerarchica
        // Il mapper gestisce ricorsivamente le risposte
        return visibleRootComments.stream()
                .map(commentMapper::toCommentoResponseDTO)
                .toList();
    }

    /**
     * Nasconde un commento per l'utente corrente.
     *
     *
     *
     * @param commentId L'ID del commento da nascondere
     * @param userId L'ID dell'utente
     * @throws ResourceNotFoundException se il commento non esiste
     */
    @Transactional
    public void nascondiCommento(Long commentId, Long userId) {
        log.info("Nascondimento commento ID: {} per utente ID: {}", commentId, userId);

        // Verifica che il commento esista
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException(ENTITY_COMMENT, FIELD_ID, commentId);
        }

        // Verifica che non sia già nascosto 
        if (hiddenCommentRepository.existsByCommentIdAndUserId(commentId, userId)) {
            log.debug("Commento ID: {} già nascosto per utente ID: {}", commentId, userId);
            return;
        }

        // Crea il record di nascondimento
        HiddenComment hiddenComment = HiddenComment.builder()
                .comment(commentRepository.getReferenceById(commentId))
                .user(userRepository.getReferenceById(userId))
                .build();

        hiddenCommentRepository.save(hiddenComment);

        log.info("Commento ID: {} nascosto per utente ID: {}", commentId, userId);
    }

    /**
     * Mostra un commento precedentemente nascosto.
     *
     * Operazione inversa di nascondiCommento.
     * Elimina il record da HiddenComment, rendendo il commento nuovamente visibile.
     *
     * @param commentId L'ID del commento da mostrare
     * @param userId L'ID dell'utente
     */
    @Transactional
    public void mostraCommento(Long commentId, Long userId) {
        log.info("Mostra commento ID: {} per utente ID: {}", commentId, userId);

        // Elimina il record di nascondimento se esiste
        hiddenCommentRepository.deleteByCommentIdAndUserId(commentId, userId);

        log.info("Commento ID: {} ora visibile per utente ID: {}", commentId, userId);
    }

    /**
     * Conta i commenti totali di un utente.
     *
     * Utile per mostrare statistiche nel profilo utente.
     * Include sia commenti principali che risposte.
     *
     * @param userId L'ID dell'utente
     * @return Il numero di commenti scritti dall'utente
     */
    @Transactional(readOnly = true)
    public long contaCommentiUtente(Long userId) {
        return commentRepository.countByUserId(userId);
    }
}