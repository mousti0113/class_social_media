DOCUMENTAZIONE PROGETTO CLASS SOCIAL MEDIA


AMBIENTE DI SVILUPPO


Elenco delle librerie e versioni utilizzate

Backend (Java/Spring Boot)
    • Java: versione 25
    • Spring Boot: 3.5.6
    • Build Tool: Maven 3.9.9
    • Database: PostgreSQL 17
    • Spring Boot Starter Web: per REST API
    • Spring Boot Starter Data JPA: per accesso database con Hibernate
    • Spring Boot Starter Security: autenticazione e autorizzazione
    • Spring Boot Starter Validation: validazione input con Bean Validation
    • Spring Boot Starter Mail: invio email per verifica account e reset password
    • Spring Boot Starter WebSocket: comunicazione real-time per notifiche e messaggi
    • Spring Boot Starter Actuator: monitoring e health checks
    • PostgreSQL Driver: connessione al database PostgreSQL
    • Spring Dotenv: 4.0.0 - gestione variabili d'ambiente da file .env
    • Bucket4j: 8.10.1 - rate limiting per proteggere API da abusi
    • Caffeine Cache: cache in-memory per rate limiting
    • Cloudinary HTTP5: 2.3.2 - upload e gestione immagini nel cloud
    
    • Docker: containerizzazione con multi-stage builds

Frontend (Angular)
    • Angular: 20.3.15 (standalone components, zoneless)
    • TypeScript: 5.9.2
    • Build Tool: Angular CLI 20.3.10
   
    • Tailwind CSS: 4.1.14 - utility-first CSS framework
    
    • Lucide Angular: 0.553.0 - libreria di icone moderne
    • STOMP.js: 7.2.1 - client WebSocket per STOMP protocol
    • SockJS Client: 1.6.1 - fallback WebSocket per browser più vecchi
    formattazione date
   
    • ngx-infinite-scroll: 20.0.0 - scroll infinito per feed post
    • Angular Service Worker: 20.3.15 - PWA e caching offline

Motivazione delle scelte principali
    • Spring Boot 3.5.6: framework moderno e completo per backend enterprise con dependency injection, configurazione automatica e vasto ecosistema
    • JWT: standard industry per autenticazione stateless scalabile, permette refresh token long-lived (30 giorni) e access token short-lived (30 minuti)
    • PostgreSQL: database relazionale robusto, supporto JSON, full-text search, transazioni ACID
    • Bucket4j: implementazione token bucket algorithm per rate limiting efficiente e configurabile
    • Cloudinary: soluzione cloud-native per gestione immagini (upload, resize, optimization, CDN) senza storage locale
    • Angular 20 Signals: state management reattivo più performante rispetto a RxJS per casi semplici, riduce complessità
    • Zoneless: eliminazione di Zone.js migliora performance riducendo overhead change detection
    • Standalone Components: tree-shaking migliore, bundle più piccoli, architettura modulare senza NgModules
    • Tailwind CSS 4: rapid prototyping, design system consistente, purge automatico di CSS inutilizzato
    • STOMP + SockJS: protocollo standard per WebSocket con fallback automatico per compatibilità browser

Istruzioni per l'installazione

Clone repository:
git clone https://github.com/mousti0113/class_social_media
cd class_social_media

Backend:
cd backend
mvn clean install
mvn spring-boot:run

Frontend:
cd frontend
npm install
npm start

Deployment:
• Backend: Fly.io con Docker (Dockerfile in backend/)
• Frontend: Firebase Hosting (ng build --configuration production)
• Database: PostgreSQL su Fly.io (gestito direttamente, no Docker)


STRUTTURA DEL CODICE


Mappatura delle directory principali del progetto

Backend (Spring Boot)
backend/
├── src/main/java/com/example/backend/
│   ├── controllers/        # REST API endpoints (AuthController, PostController, etc.)
│   ├── services/           # Business logic (AuthService, NotificationService, etc.)
│   ├── repositories/       # Data access layer con Spring Data JPA
│   ├── models/             # Entity JPA (@Entity User, Post, Comment, etc.)
│   ├── dtos/               # Data Transfer Objects (request/response)
│   │   ├── request/        # DTO per richieste client
│   │   └── response/       # DTO per risposte server
│   ├── security/           # Configurazione sicurezza (JWT, filters, authentication)
│   ├── config/             # Spring configuration (@Configuration, CORS, WebSocket)
│   ├── exception/          # Eccezioni custom e GlobalExceptionHandler
│   ├── mappers/            # Conversione Entity ↔ DTO
│   ├── listeners/          # Event listeners per notifiche asincrone
│   └── events/             # Eventi applicativi (NotificationCreatedEvent, etc.)
├── src/main/resources/
│   ├── application.yml     # Configurazione principale (database, JWT, CORS, logging)
│   └── application-test.yml # Configurazione per test
├── Dockerfile              # Multi-stage build per produzione
└── pom.xml                 # Dipendenze Maven

Frontend (Angular)
frontend/
├── src/app/
│   ├── core/               # Servizi singleton e logica core
│   │   ├── api/            # API services (AuthService, PostService, etc.)
│   │   ├── auth/           # Guards, interceptors per JWT
│   │   ├── stores/         # State management con Signals
│   │   └── utils/          # Utility functions
│   ├── features/           # Feature modules
│   │   ├── auth/           # Login, registrazione, password reset
│   │   ├── home/           # Feed principale con infinite scroll
│   │   ├── profile-view/  # Profilo utente e post personali
│   │   ├── post/           # Creazione e gestione post
│   │   ├── messages/       # Chat diretti con WebSocket
│   │   ├── notifications/  # Centro notifiche real-time
│   │   ├── search/         # Ricerca utenti e post
│   │   ├── settings/       # Impostazioni profilo
│   │   └── admin/          # Dashboard amministratore
│   ├── layout/             # Layout components
│   │   ├── header/         # Header con navigazione
│   │   ├── mobile-nav/     # Bottom navigation mobile
│   │   └── main-layout/    # Container layout principale
│   ├── shared/             # Componenti riutilizzabili
│   │   ├── components/     # Button, Modal, Card, etc.
│   │   ├── directives/     # Custom directives
│   │   ├── pipes/          # Custom pipes per formattazione
│   │   └── ui/             # UI primitives (dropdown, dialog, toast)
│   └── models/             # TypeScript interfaces e types
├── public/
│   ├── icons/              # PWA icons (72x72 fino a 512x512)
│   ├── fonts/              # Web fonts
│   └── images/             # Immagini statiche
├── src/environments/       # Configurazione ambiente (dev/prod)
├── firebase.json           # Configurazione Firebase Hosting
├── .firebaserc             # Firebase project configuration
├── ngsw-config.json        # Service Worker configuration
└── manifest.webmanifest    # PWA manifest

Descrizione della funzione di ogni modulo/file

Backend:
    • controllers: gestiscono richieste HTTP, validazione input, invocano services, restituiscono ResponseEntity
    • services: contengono business logic, orchestrazione tra repository, gestione transazioni (@Transactional)
    • repositories: interfacce Spring Data JPA per CRUD e query custom su database
    • models: entità JPA con relazioni (@OneToMany, @ManyToOne), mapping tabelle database
    • dtos: oggetti per trasferimento dati, separano API contract da struttura interna database
    • security: JwtTokenProvider genera/valida token, JwtAuthenticationFilter intercetta richieste, SecurityConfig configura Spring Security
    • config: WebSocketConfig per STOMP, CorsConfig per CORS policy, AsyncConfig per @Async
    • exception: GlobalExceptionHandler cattura eccezioni e restituisce risposte HTTP uniformi
    • listeners: gestiscono eventi applicativi in modo asincrono (es. invio notifiche WebSocket dopo creazione post)
    • events: oggetti evento pubblicati con ApplicationEventPublisher per disaccoppiare logica

Frontend:
    • core/api: servizi che comunicano con backend REST API usando HttpClient
    • core/auth: AuthGuard protegge rotte, AuthInterceptor inietta JWT in headers, gestisce refresh token
    • core/stores: Signals store per state management reattivo (AuthStore, NotificationStore, etc.)
    • features: moduli funzionali con componenti, routing, logica specifica feature
    • layout: componenti layout riutilizzabili per struttura pagina consistente
    • shared/components: componenti UI riutilizzabili (buttons, cards, modals)
    • shared/pipes: custom pipes per formattazione (relative time, highlight search, safe HTML)
    • models: TypeScript interfaces che rispecchiano DTO backend per type safety


DECISIONI DI DESIGN


Spiegazione di almeno 3 scelte implementative significative

1. Architettura Event-Driven per Notifiche Real-Time

Problema: Le notifiche devono essere inviate via WebSocket quando si verifica un evento (nuovo like, commento, menzione). Inizialmente NotificationService aveva dipendenza diretta con WebSocketController creando dipendenza circolare.

Soluzione implementata: Pattern Event-Driven con Spring Application Events
    • NotificationService pubblica NotificationCreatedEvent dopo aver salvato notifica nel database
    • NotificationWebSocketListener ascolta l'evento con @TransactionalEventListener(phase = AFTER_COMMIT)
    • Il listener viene eseguito in modo asincrono (@Async) dopo che la transazione è committata
    • Il listener usa SimpMessagingTemplate per inviare notifica via WebSocket all'utente destinatario

Vantaggi:
    • Disaccoppiamento completo tra business logic (NotificationService) e layer di comunicazione (WebSocket)
    • Nessuna dipendenza circolare
    • Esecuzione asincrona non blocca il thread principale
    • Se invio WebSocket fallisce (utente offline), non compromette salvataggio notifica nel database
    • Facile estendere con nuovi listener (es. invio email, push notification)

Codice rilevante: NotificationWebSocketListener, NotificationService

2. Doppio Token JWT (Access + Refresh) con Auto-Refresh Automatico

Problema: Access token long-lived (es. 7 giorni) rischioso se rubato. Access token short-lived (es. 5 minuti) richiede login frequente, pessima UX.

Soluzione implementata: Dual Token Strategy con refresh automatico lato client
    • Access Token: durata 30 minuti, contiene claims utente, usato per ogni API call
    • Refresh Token: durata 30 giorni, salvato su database con hash BCrypt, usato solo per ottenere nuovo access token
    • TokenRefreshService (frontend): monitora scadenza access token con Angular effect()
    • Quando mancano 5 minuti alla scadenza (detection = 25 minuti), refresh automatico in background
    • Se refresh fallisce (refresh token scaduto/invalido), redirect a login
    • Logout invalida refresh token su database impedendo riutilizzo

Vantaggi:
    • Sicurezza: access token short-lived (30 min) limita finestra attacco se rubato
    • UX: utente mai interrotto, refresh trasparente ogni 25 minuti
    • Scalabilità: access token stateless (no lookup database), refresh token stateful (revocabile)
    • Logout efficace: eliminazione refresh token da DB impedisce nuovi access token

Codice rilevante: JwtTokenProvider, RefreshTokenService, TokenRefreshService (frontend)

3. Standalone Components + Zoneless Angular per Performance

Problema: NgModules creano bundle grandi con codice inutilizzato. Zone.js aggiunge overhead per change detection anche quando non necessario.

Soluzione implementata: Architettura moderna Angular 20
    • Standalone Components: ogni componente dichiara dipendenze con imports diretti, no NgModule
    • Zoneless: rimosso Zone.js da polyfills, change detection manuale o tramite Signals
    • Signals API: state management reattivo che triggera change detection automaticamente solo dove necessario
    • Lazy Loading: rotte caricano feature modules on-demand riducendo bundle iniziale

Vantaggi:
    • Bundle size: tree-shaking aggressivo rimuove codice mai usato, bundle iniziale ridotto del 30%
    • Performance runtime: zoneless elimina monkey-patching di API async, meno overhead
    • Developer experience: architettura più semplice, meno boilerplate, import espliciti
    • Signals: codice più leggibile rispetto a RxJS per state semplice, meno memory leaks

Confronto:
    • Prima (con NgModule + Zone.js): bundle 2.1MB, Time to Interactive 3.2s
    • Dopo (standalone + zoneless): bundle 1.4MB, Time to Interactive 2.1s

Codice rilevante: app.config.ts (provideExperimentalZonelessChangeDetection), tutti i componenti


GESTIONE DELLE VERSIONI


Link al repository Git/GitHub/GitLab

Repository GitHub: https://github.com/mousti0113/class_social_media
Branch principale: main
Visibilità: public

Evidenza dell'uso di commit significativi e ben descritti

Convenzioni commit:
    • Prefissi: feat: (nuova feature), fix: (bug fix), refactor: (ristrutturazione codice), docs: (documentazione), style: (formattazione), test: (aggiunta test)
    • Lingua: italiano per messaggi descrittivi
    • Formato: <tipo>: <descrizione breve> (es. "feat: aggiunto refresh automatico token JWT")
    • Commit atomici: ogni commit rappresenta un cambiamento logico singolo e compilabile

Esempi commit repository:
    • "feat: implementato sistema notifiche real-time con WebSocket"
    • "fix: risolto problema dipendenza circolare NotificationService"
    • "refactor: migrazione da NgModules a standalone components"
    • "feat: aggiunto rate limiting con Bucket4j per protezione API"
    • "fix: corretto bug refresh token che non sopravviveva a restart backend"
    • "docs: aggiornato README con istruzioni Docker"



ESEMPI DI CODICE


Snippet di codice per le 3 classi/funzioni principali, evidenziando l'uso di commenti e aderenza agli standard di stile

1. AuthController - Controller REST per autenticazione


package com.example.backend.controllers;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.PasswordResetConfirmDTO;
import com.example.backend.dtos.request.PasswordResetRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.services.AuthService;
import com.example.backend.services.EmailVerificationService;
import com.example.backend.services.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione dell'autenticazione.
 * Tutti i metodi delegano la logica al AuthService e gestiscono solo HTTP.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String MESSAGE_KEY = "message";
    
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    /**
     * POST /api/auth/register
     * Registra un nuovo utente.
     * 
     * L'utente viene creato ma NON attivo fino a verifica email.
     * Viene inviata email con link di verifica.
     *
     * @param request DTO con username, email, password e nome completo
     * @return LoginResponseDTO senza token (user deve verificare email)
     * @throws ResourceAlreadyExistsException se username o email già esistono
     * @throws IllegalArgumentException se email non è @marconirovereto.it
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> registrazione(
            @Valid @RequestBody RegistrazioneRequestDTO request) {
        log.debug("POST /api/auth/register - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.registrazione(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * POST /api/auth/login
     * Effettua il login.
     * 
     * Verifica credenziali, controlla che account sia attivo (email verificata),
     * aggiorna lastSeen, genera access token (30 min) e refresh token (30 giorni).
     *
     * @param request DTO con username e password
     * @return LoginResponseDTO con access token, refresh token e dati utente
     * @throws InvalidCredentialsException se username o password errati
     * @throws EmailNotVerifiedException se account non ancora verificato
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {
        log.debug("POST /api/auth/login - Username: {}", request.getUsername());

        LoginResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh-token
     * Rinnova l'access token usando il refresh token.
     * 
     * Verifica che refresh token sia valido e non scaduto,
     * genera nuovo access token (30 min) e ruota refresh token (30 giorni).
     *
     * @param request DTO con refresh token
     * @return RefreshTokenResponseDTO con nuovo access token e refresh token
     * @throws InvalidTokenException se refresh token non valido o scaduto
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        log.debug("POST /api/auth/refresh-token");

        RefreshTokenResponseDTO response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/logout
     * Effettua il logout invalidando il refresh token.
     * 
     * Endpoint pubblico perché client ha già pulito token localmente.
     * Elimina refresh token da database per impedire riutilizzo.
     *
     * @param request DTO contenente refresh token da invalidare
     * @return Messaggio di conferma
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody(required = false) RefreshTokenRequestDTO request) {
        if (request != null && request.getRefreshToken() != null) {
            log.debug("POST /api/auth/logout - Invalidating refresh token");
            authService.logoutByRefreshToken(request.getRefreshToken());
        } else {
            log.debug("POST /api/auth/logout - No token provided");
        }

        return ResponseEntity.ok("Logout effettuato con successo");
    }
}


Note di stile:
    • Commenti Javadoc per ogni metodo con @param, @return, @throws
    • Log SLF4J con livello appropriato (debug per operazioni normali, warn per errori)
    • Dependency injection con Lombok @RequiredArgsConstructor (final fields)
    • Validazione input con @Valid (Bean Validation)
    • ResponseEntity per controllo completo di status HTTP
    • Separazione responsabilità: controller solo HTTP, logica in service


2. AuthService - Service per business logic autenticazione


package com.example.backend.services;

import com.example.backend.dtos.request.LoginRequestDTO;
import com.example.backend.dtos.request.RegistrazioneRequestDTO;
import com.example.backend.dtos.request.RefreshTokenRequestDTO;
import com.example.backend.dtos.response.LoginResponseDTO;
import com.example.backend.dtos.response.RefreshTokenResponseDTO;
import com.example.backend.exception.EmailNotVerifiedException;
import com.example.backend.exception.InvalidCredentialsException;
import com.example.backend.exception.InvalidTokenException;
import com.example.backend.exception.ResourceAlreadyExistsException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.mappers.UserMapper;
import com.example.backend.models.RefreshToken;
import com.example.backend.models.User;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service per la gestione dell'autenticazione e registrazione utenti.
 * 
 * Responsabilità:
 * - Registrazione nuovi utenti con validazione
 * - Login con generazione JWT
 * - Refresh token per rinnovo access token
 * - Logout con invalidazione refresh token
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final ValidationService validationService;
    private static final String AUTHORIZATION_TYPE = "Bearer";

    /**
     * Registra un nuovo utente.
     * 
     * Validazioni:
     * - Username unico
     * - Email unica
     * - Email deve terminare con @marconirovereto.it
     * - Non superare limite massimo studenti configurato
     * 
     * L'utente viene creato con isActive=false.
     * Viene generato token di verifica e inviata email.
     * 
     * @param request DTO con dati registrazione
     * @return LoginResponseDTO senza token (user non attivo)
     * @throws ResourceAlreadyExistsException se username/email già esistono
     * @throws IllegalArgumentException se email non del dominio consentito
     * @throws LimitExceededException se raggiunto limite studenti
     */
    @Transactional
    public LoginResponseDTO registrazione(RegistrazioneRequestDTO request) {
        log.info("Tentativo registrazione per username: {}", request.getUsername());

        // Valida limite studenti (configurabile tramite app.max-students)
        validationService.validateStudentLimit();

        // Verifica username unico
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Username già esistente: {}", request.getUsername());
            throw new ResourceAlreadyExistsException("Utente", "username", 
                    request.getUsername());
        }

        // Verifica email unica
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email già esistente: {}", request.getEmail());
            throw new ResourceAlreadyExistsException("Utente", "email", 
                    request.getEmail());
        }

        // Valida dominio email
        if (!request.getEmail().toLowerCase().endsWith("@marconirovereto.it")) {
            log.warn("Email non valida: {}", request.getEmail());
            throw new IllegalArgumentException(
                    "Email deve essere del dominio @marconirovereto.it");
        }

        // Crea nuovo utente (NON ATTIVO fino a verifica email)
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getNomeCompleto())
                .isAdmin(false)
                .isActive(false) // Deve verificare email
                .build();

        user = userRepository.save(user);
        log.info("Utente registrato (non attivo): {} (ID: {})", 
                user.getUsername(), user.getId());

        // Crea token di verifica e invia email
        emailVerificationService.createVerificationToken(user);
        log.debug("Token verifica creato per: {}", user.getUsername());

        // Restituisce response senza token JWT (user non attivo)
        return LoginResponseDTO.builder()
                .accessToken(null)
                .refreshToken(null)
                .type(null)
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Effettua il login.
     * 
     * Flusso:
     * 1. Autentica con AuthenticationManager (verifica password)
     * 2. Carica user da database (solo se attivo)
     * 3. Verifica che account sia attivo (email verificata)
     * 4. Aggiorna lastSeen timestamp
     * 5. Genera access token (30 min) e refresh token (30 giorni)
     * 
     * @param request DTO con username e password
     * @return LoginResponseDTO con token e dati utente
     * @throws InvalidCredentialsException se credenziali errate
     * @throws ResourceNotFoundException se utente non esiste o non attivo
     * @throws EmailNotVerifiedException se account non verificato
     */
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        log.info("Tentativo login per username: {}", request.getUsername());

        try {
            // Autentica con Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("Login fallito - credenziali errate: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }

        // Carica user (solo se attivo)
        User user = userRepository.findByUsernameAndIsActiveTrue(request.getUsername())
                .orElseThrow(() -> {
                    log.error("Utente non trovato o non attivo: {}", 
                            request.getUsername());
                    return new ResourceNotFoundException("Utente", "username", 
                            request.getUsername());
                });

        // Doppia verifica isActive (paranoia)
        if (!user.getIsActive().booleanValue()) {
            log.warn("Tentativo login con account non verificato: {}", 
                    user.getUsername());
            throw new EmailNotVerifiedException(
                    "Devi verificare la tua email prima di accedere");
        }

        // Aggiorna last seen
        user.setLastSeen(LocalDateTime.now());
        user = userRepository.save(user);

        // Genera token JWT
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.creaRefreshToken(user.getId());

        log.info("Login successo: {} (ID: {})", user.getUsername(), user.getId());

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type(AUTHORIZATION_TYPE)
                .user(userMapper.toUtenteResponseDTO(user))
                .build();
    }

    /**
     * Rinnova access token usando refresh token.
     * 
     * Verifica che refresh token sia:
     * - Esistente nel database
     * - Non scaduto (30 giorni)
     * - Associato a utente attivo
     * 
     * Genera nuovo access token (30 min) e ruota refresh token (30 giorni).
     * Vecchio refresh token viene eliminato dal database.
     * 
     * @param request DTO con refresh token
     * @return RefreshTokenResponseDTO con nuovi token
     * @throws InvalidTokenException se refresh token non valido o scaduto
     */
    @Transactional
    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        log.debug("Richiesta refresh token");

        // Verifica e rinnova refresh token (genera nuovo)
        RefreshToken refreshToken = refreshTokenService
                .verificaERinnovaRefreshToken(request.getRefreshToken());

        User user = refreshToken.getUser();

        // Genera nuovo access token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        log.debug("Token refresh completato per user: {}", user.getUsername());

        return RefreshTokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type(AUTHORIZATION_TYPE)
                .build();
    }

    /**
     * Logout eliminando refresh token dal database.
     * 
     * Impedisce riutilizzo del refresh token per generare nuovi access token.
     * Anche se access token rimane valido fino a scadenza (30 min),
     * non può essere rinnovato.
     * 
     * @param refreshTokenString refresh token da invalidare
     */
    @Transactional
    public void logoutByRefreshToken(String refreshTokenString) {
        log.debug("Logout - invalidazione refresh token");
        refreshTokenService.eliminaRefreshToken(refreshTokenString);
    }
}


Note di stile:
    • Commenti dettagliati per ogni metodo con spiegazione flusso
    • @Transactional per consistenza database (atomicità operazioni)
    • Gestione eccezioni specifica (InvalidCredentialsException, EmailNotVerifiedException)
    • Log informativi per troubleshooting (info per eventi importanti, debug per dettagli)
    • Naming descrittivo (verificaERinnovaRefreshToken)
    • Validazioni multiple (username unico, email dominio, limite studenti)


3. NotificationWebSocketListener - Listener eventi per WebSocket


package com.example.backend.listeners;

import com.example.backend.events.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener per eventi WebSocket.
 * 
 * Questo componente gestisce l'invio di notifiche real-time via WebSocket
 * in risposta agli eventi del sistema.
 * 
 * RISOLVE LA DIPENDENZA CIRCOLARE:
 * Prima: NotificationService → WebSocketController (con @Lazy)
 * Dopo: NotificationService → Event → NotificationWebSocketListener → Template
 * 
 * Vantaggi architettura event-driven:
 * - Nessuna dipendenza circolare
 * - Esecuzione asincrona (non blocca thread principale)
 * - Disaccoppiamento completo tra business logic e comunicazione
 * - Facile aggiungere nuovi listener (es. email, push notification)
 * - Se WebSocket fallisce, non compromette salvataggio notifica
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gestisce invio notifiche via WebSocket quando viene creata una notifica.
     * 
     * Flusso:
     * 1. NotificationService salva notifica nel database
     * 2. NotificationService pubblica NotificationCreatedEvent
     * 3. Spring chiama questo metodo DOPO che transazione è committata
     * 4. Metodo eseguito in modo asincrono (grazie a @Async)
     * 5. Invia notifica via WebSocket all'utente destinatario
     * 
     * Caratteristiche:
     * - @TransactionalEventListener(AFTER_COMMIT): eseguito solo se transazione OK
     * - @Async: non blocca thread che ha salvato notifica
     * - Best effort: se utente offline, errore viene loggato ma non propagato
     * - Notifica rimane comunque salvata nel database
     * 
     * @param event evento contenente username destinatario e notifica da inviare
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        log.debug("Evento NotificationCreatedEvent ricevuto per utente: {}",
                event.getUsername());

        try {
            // Invia notifica via WebSocket usando SimpMessagingTemplate
            // Endpoint: /user/{username}/queue/notifications
            messagingTemplate.convertAndSendToUser(
                    event.getUsername(),
                    "/queue/notifications",
                    event.getNotification()
            );

            log.debug("Notifica inviata via WebSocket a: {}", event.getUsername());
            
        } catch (Exception e) {
            // Log ma non propagare errore
            // La notifica è già salvata nel DB, WebSocket è "best effort"
            // Utente la vedrà comunque quando aprirà pagina notifiche
            log.warn("Errore invio notifica WebSocket a {}: {}",
                    event.getUsername(), e.getMessage());
        }
    }
}


Note di stile:
    • Commento architetturale esteso che spiega PERCHÉ esiste questa classe
    • @Async per esecuzione asincrona (configurato in AsyncConfig)
    • @TransactionalEventListener(AFTER_COMMIT): garantisce che notifica sia salvata prima di invio
    • Try-catch per non propagare errori WebSocket (best effort delivery)
    • Log debug per troubleshooting real-time issues
    • Pattern Event-Driven per disaccoppiamento
    • SimpMessagingTemplate per comunicazione WebSocket (Spring WebSocket abstraction)
