package com.example.backend.services;

import com.example.backend.dtos.request.CreaPostRequestDTO;
import com.example.backend.dtos.request.ModificaPostRequestDTO;
import com.example.backend.dtos.response.PostDettaglioResponseDTO;
import com.example.backend.dtos.response.PostResponseDTO;
import com.example.backend.exception.InvalidInputException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.mappers.PostMapper;
import com.example.backend.models.HiddenPost;
import com.example.backend.models.Post;
import com.example.backend.models.User;
import com.example.backend.repositories.HiddenPostRepository;
import com.example.backend.repositories.PostRepository;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service per la gestione dei post.
 * Gestisce tutte le operazioni CRUD sui post, il feed, la ricerca e le funzionalità di nascondere/mostrare.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final HiddenPostRepository hiddenPostRepository;
    private final PostMapper postMapper;

    /**
     * Crea un nuovo post.
     * <p>
     * Questo metodo gestisce la creazione di un nuovo post da parte di un utente.
     * Un post può contenere solo testo, solo un'immagine, oppure entrambi.
     * La validazione assicura che almeno uno dei due sia presente.
     * <p>
     * Flusso:
     * 1. Carica l'utente autore dal database
     * 2. Valida che ci sia contenuto o immagine (o entrambi)
     * 3. Crea l'entità Post
     * 4. Salva nel database
     * 5. Converte in DTO e restituisce
     *
     * @param userId  L'ID dell'utente che sta creando il post
     * @param request Il DTO con contenuto e/o URL immagine
     * @return PostResponseDTO con i dati del post appena creato
     * @throws ResourceNotFoundException se l'utente non esiste
     * @throws InvalidInputException     se mancano sia contenuto che immagine
     */
    @Transactional
    public PostResponseDTO creaPost(Long userId, CreaPostRequestDTO request) {
        log.info("Creazione nuovo post per utente ID: {}", userId);

        // Carica l'utente che sta creando il post
        User autore = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente", "id", userId));

        // Valida che ci sia almeno contenuto o immagine
        if (!request.isValido()) {
            log.warn("Tentativo di creare post senza contenuto né immagine - Utente ID: {}", userId);
            throw new InvalidInputException("Il post deve contenere almeno del testo o un'immagine");
        }

        // Crea l'entità Post

        Post post = Post.builder()
                .user(autore)
                .content(request.getContenuto())
                .imageUrl(request.getImageUrl())
                .isDeletedByAuthor(false)
                .build();

        // Salva nel database
        post = postRepository.save(post);
        log.info("Post creato con successo - ID: {} da utente: {}", post.getId(), autore.getUsername());

        // Converti in DTO e restituisci

        return postMapper.toPostResponseDTO(post, userId);
    }

    /**
     * Modifica un post esistente.
     * <p>
     * Questo metodo permette all'autore di modificare il contenuto testuale di un post.
     * L'immagine non può essere modificata.
     * Solo l'autore del post può modificarlo.
     *
     * @param postId  L'ID del post da modificare
     * @param userId  L'ID dell'utente che richiede la modifica
     * @param request Il DTO con il nuovo contenuto
     * @return PostResponseDTO con i dati aggiornati
     * @throws ResourceNotFoundException se il post non esiste
     * @throws UnauthorizedException     se l'utente non è l'autore del post
     */
    @Transactional
    public PostResponseDTO modificaPost(Long postId, Long userId, ModificaPostRequestDTO request) {
        log.info("Modifica post ID: {} da utente ID: {}", postId, userId);

        // Carica il post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Verifica che l'utente sia l'autore del post

        if (!post.getUser().getId().equals(userId)) {
            log.warn("Tentativo di modifica post non autorizzato - Post ID: {}, Utente ID: {}", postId, userId);
            throw new UnauthorizedException("Non hai i permessi per modificare questo post");
        }
        boolean isDeletedByAuthor = post.getIsDeletedByAuthor();
        // Verifica che il post non sia stato cancellato
        if (isDeletedByAuthor) {
            log.warn("Tentativo di modificare post cancellato - Post ID: {}", postId);
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        // Aggiorna il contenuto

        if (request.getContenuto() != null) {
            post.setContent(request.getContenuto());
        }

        // Salva le modifiche

        post = postRepository.save(post);
        log.info("Post modificato con successo - ID: {}", postId);

        return postMapper.toPostResponseDTO(post, userId);
    }

    /**
     * Elimina un post (soft delete).
     * <p>
     * Questo metodo non elimina fisicamente il post dal database, ma lo marca come cancellato
     * impostando isDeletedByAuthor = true.
     *
     * @param postId L'ID del post da eliminare
     * @param userId L'ID dell'utente che richiede l'eliminazione
     * @throws ResourceNotFoundException se il post non esiste
     * @throws UnauthorizedException     se l'utente non è l'autore del post
     */
    @Transactional
    public void eliminaPost(Long postId, Long userId) {
        log.info("Eliminazione post ID: {} da utente ID: {}", postId, userId);

        // Carica il post
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Verifica che l'utente sia l'autore o un admin

        if (!post.getUser().getId().equals(userId)) {
            log.warn("Tentativo di eliminazione post non autorizzato - Post ID: {}, Utente ID: {}", postId, userId);
            throw new UnauthorizedException("Non hai i permessi per eliminare questo post");
        }

        // Soft delete: marca il post come cancellato invece di eliminarlo fisicamente
        post.setIsDeletedByAuthor(true);
        postRepository.save(post);

        log.info("Post eliminato con successo (soft delete) - ID: {}", postId);
    }

    /**
     * Ottiene i dettagli completi di un singolo post.
     * <p>
     * <p>
     * Il metodo verifica anche che l'utente non abbia nascosto questo post.
     * Se lo ha nascosto, il post non viene mostrato come se non esistesse.
     *
     * @param postId L'ID del post da visualizzare
     * @param userId L'ID dell'utente che sta visualizzando (per verificare like e post nascosti)
     * @return PostDettaglioResponseDTO con tutti i dettagli, inclusi i commenti
     * @throws ResourceNotFoundException se il post non esiste o è nascosto dall'utente
     */
    @Transactional(readOnly = true)
    public PostDettaglioResponseDTO ottieniPost(Long postId, Long userId) {
        log.debug("Caricamento dettagli post ID: {} per utente ID: {}", postId, userId);

        // Usa la query ottimizzata che carica tutto con JOIN FETCH
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Verifica che l'utente non abbia nascosto questo post
        if (hiddenPostRepository.existsByPostIdAndUserId(postId, userId)) {
            log.debug("Post ID: {} è nascosto per utente ID: {}", postId, userId);
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        // Converte in DTO con tutti i dettagli
        return postMapper.toPostDettaglioResponseDTO(post, userId);
    }

    /**
     * Ottiene il feed dei post visibili per l'utente.
     * <p>
     * Questo è il metodo principale per la home page del social network.
     * Restituisce tutti i post visibili all'utente, escludendo:
     * - Post cancellati (isDeletedByAuthor = true)
     * - Post nascosti dall'utente
     * <p>
     * I risultati sono paginati per evitare di caricare troppi post in una volta.
     * L'ordinamento è per data di creazione decrescente: i post più recenti appaiono prima.
     *
     * @param userId   L'ID dell'utente che sta visualizzando il feed
     * @param pageable Parametri di paginazione (numero pagina, dimensione, ordinamento)
     * @return Page di PostResponseDTO con i post del feed
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> ottieniFeed(Long userId, Pageable pageable) {
        log.debug("Caricamento feed per utente ID: {} - Pagina: {}", userId, pageable.getPageNumber());

        // La query findVisiblePostsForUser filtra automaticamente post cancellati e nascosti
        Page<Post> posts = postRepository.findVisiblePostsForUser(userId, pageable);

        log.debug("Feed caricato: {} post trovati per utente ID: {}", posts.getTotalElements(), userId);

        // Converte ogni post in DTO

        return posts.map(post -> postMapper.toPostResponseDTO(post, userId));
    }

    /**
     * Cerca post per parola chiave.
     * <p>
     * <p>
     * La ricerca è case-insensitive
     *
     * @param searchTerm La parola o frase da cercare
     * @param userId     L'ID dell'utente che sta cercando
     * @param pageable   Parametri di paginazione
     * @return Page di PostResponseDTO con i risultati della ricerca
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> cercaPost(String searchTerm, Long userId, Pageable pageable) {
        log.debug("Ricerca post con termine: '{}' per utente ID: {}", searchTerm, userId);

        // Valida che il termine di ricerca non sia vuoto
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            log.warn("Tentativo di ricerca con termine vuoto");
            throw new InvalidInputException("Il termine di ricerca non può essere vuoto");
        }

        // Esegue la ricerca
        Page<Post> posts = postRepository.searchPosts(searchTerm.trim(), pageable);

        log.debug("Ricerca completata: {} risultati per termine '{}'", posts.getTotalElements(), searchTerm);

        return posts.map(post -> postMapper.toPostResponseDTO(post, userId));
    }

    /**
     * Ottiene i post di un utente specifico.
     * <p>
     * Questo metodo viene usato quando visiti il profilo di un altro studente.
     * Mostra tutti i suoi post pubblici, escludendo:
     * - Post cancellati dall'autore
     * - Post che l'utente corrente ha nascosto
     *
     * @param authorId      L'ID dell'utente di cui vogliamo vedere i post
     * @param currentUserId L'ID dell'utente che sta visualizzando
     * @param pageable      Parametri di paginazione
     * @return Page di PostResponseDTO con i post dell'utente
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> ottieniPostUtente(Long authorId, Long currentUserId, Pageable pageable) {
        log.debug("Caricamento post dell'utente ID: {} per utente ID: {}", authorId, currentUserId);

        // Verifica che l'utente autore esista
        if (!userRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Utente", "id", authorId);
        }

        // La query filtra automaticamente post cancellati e nascosti dall'utente corrente
        Page<Post> posts = postRepository.findVisiblePostsByUser(authorId, currentUserId, pageable);

        log.debug("Trovati {} post per utente ID: {}", posts.getTotalElements(), authorId);

        return posts.map(post -> postMapper.toPostResponseDTO(post, currentUserId));
    }

    /**
     * Nasconde un post per l'utente corrente.
     * <p>
     * Quando nascondi un post, questo non viene eliminato né modificato.
     * Semplicemente viene creato un record nella tabella HiddenPost.
     * <p>
     * Dopo aver nascosto un post:
     * - Non apparirà più nel tuo feed
     * - Non lo vedrai nel profilo dell'autore
     * - Altri utenti continueranno a vederlo normalmente
     *
     * @param postId L'ID del post da nascondere
     * @param userId L'ID dell'utente che vuole nascondere il post
     * @throws ResourceNotFoundException se il post non esiste
     */
    @Transactional
    public void nascondiPost(Long postId, Long userId) {
        log.info("Nascondimento post ID: {} per utente ID: {}", postId, userId);

        // Verifica che il post esista
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        // Verifica che il post non sia già nascosto
        if (hiddenPostRepository.existsByPostIdAndUserId(postId, userId)) {
            log.debug("Post ID: {} già nascosto per utente ID: {}", postId, userId);
            return;
        }

        // Crea il record di nascondimento
        HiddenPost hiddenPost = HiddenPost.builder()
                .post(postRepository.getReferenceById(postId)) // getReferenceById evita una query extra
                .user(userRepository.getReferenceById(userId))
                .build();

        hiddenPostRepository.save(hiddenPost);

        log.info("Post ID: {} nascosto per utente ID: {}", postId, userId);
    }

    /**
     * Mostra un post precedentemente nascosto.
     * <p>
     * Questo è l'operazione inversa di nascondiPost.
     * Elimina il record dalla tabella HiddenPost, rendendo il post nuovamente visibile all'utente.
     *
     * @param postId L'ID del post da mostrare di nuovo
     * @param userId L'ID dell'utente
     */
    @Transactional
    public void mostraPost(Long postId, Long userId) {
        log.info("Mostra post ID: {} per utente ID: {}", postId, userId);

        // Elimina il record di nascondimento se esiste

        hiddenPostRepository.deleteByPostIdAndUserId(postId, userId);

        log.info("Post ID: {} ora visibile per utente ID: {}", postId, userId);
    }

    /**
     * Ottiene le statistiche dei post di un utente.
     * <p>
     * Questo metodo è un helper per mostrare nel profilo quanti post ha pubblicato un utente.
     * Usa la query di conteggio ottimizzata invece di caricare tutti i post.
     *
     * @param userId L'ID dell'utente
     * @return Il numero di post pubblicati
     */
    @Transactional(readOnly = true)
    public long contaPostUtente(Long userId) {
        return postRepository.countByUserId(userId);
    }
}