PIANO DI COLLAUDO E TESTING


MATRICE DI COLLAUDO

Associazione tra Requisiti Funzionali e Test Case specifici


AUTENTICAZIONE E GESTIONE ACCOUNT

RF-01: Registrazione Utente
Test Case Associati:
    • TC-01.1: Registrazione con dati validi
        ◦ Input: username="testuser", email="test@marconirovereto.it", password="test123", fullName="Test User"
        ◦ Expected: Status 201, access token JWT, refresh token, dati utente creati
    • TC-01.2: Registrazione con username già esistente
        ◦ Input: username già presente nel database
        ◦ Expected: Status 409 Conflict, messaggio "Username già in uso"
    • TC-01.3: Registrazione con email già esistente
        ◦ Input: email già presente nel database
        ◦ Expected: Status 409 Conflict, messaggio "Email già in uso"
    • TC-01.4: Registrazione con email non dominio marconirovereto.it
        ◦ Input: email="test@gmail.com"
        ◦ Expected: Status 400 Bad Request, messaggio errore dominio
    • TC-01.5: Registrazione con password troppo corta
        ◦ Input: password="12345" (< 6 caratteri)
        ◦ Expected: Status 400 Bad Request, errore validazione password
    • TC-01.6: Verifica rate limiting registrazioni
        ◦ Input: 6 richieste registrazione consecutive dallo stesso IP in 1 minuto
        ◦ Expected: Prime 5 OK, sesta Status 429 Too Many Requests

RF-02: Login Utente
Test Case Associati:
    • TC-02.1: Login con credenziali valide
        ◦ Input: username="pippo", password="pluto"
        ◦ Expected: Status 200, access token, refresh token, dati utente, lastSeen aggiornato
    • TC-02.2: Login con username inesistente
        ◦ Input: username="nonEsiste"
        ◦ Expected: Status 401 Unauthorized, messaggio "Credenziali non valide"
    • TC-02.3: Login con password errata
        ◦ Input: username corretto, password errata
        ◦ Expected: Status 401 Unauthorized, messaggio "Credenziali non valide"
    • TC-02.4: Login con account disattivato
        ◦ Input: credenziali corrette ma isActive=false
        ◦ Expected: Status 403 Forbidden, messaggio "Account disattivato"
    • TC-02.5: Verifica rate limiting login
        ◦ Input: 6 tentativi login dallo stesso IP in 1 minuto
        ◦ Expected: Prime 5 OK/fail, sesta Status 429 Too Many Requests

RF-03: Refresh Token
Test Case Associati:
    • TC-03.1: Refresh token valido
        ◦ Input: refresh token esistente e non scaduto
        ◦ Expected: Status 200, nuovo access token, stesso refresh token
    • TC-03.2: Refresh token scaduto
        ◦ Input: refresh token con expiry > 30 giorni fa
        ◦ Expected: Status 401 Unauthorized, messaggio "Token scaduto"
    • TC-03.3: Refresh token inesistente
        ◦ Input: token UUID random non nel database
        ◦ Expected: Status 401 Unauthorized, messaggio "Token non valido"

RF-04: Logout
Test Case Associati:
    • TC-04.1: Logout con refresh token valido
        ◦ Input: refresh token esistente
        ◦ Expected: Status 200, refresh token eliminato dal database, session chiusa
    • TC-04.2: Logout senza refresh token nel body
        ◦ Input: body vuoto o null
        ◦ Expected: Status 200, messaggio "Logout effettuato con successo"

RF-05: Reset Password
Test Case Associati:
    • TC-05.1: Richiesta reset con email esistente
        ◦ Input: email registrata nel sistema
        ◦ Expected: Status 200, token generato, email inviata, messaggio generico
    • TC-05.2: Richiesta reset con email inesistente
        ◦ Input: email non registrata
        ◦ Expected: Status 200, messaggio generico (anti user enumeration), nessuna email inviata
    • TC-05.3: Validazione token reset valido
        ◦ Input: token UUID esistente e non scaduto
        ◦ Expected: Status 200, validità true
    • TC-05.4: Validazione token reset scaduto
        ◦ Input: token con expiry > 1 ora fa
        ◦ Expected: Status 400 Bad Request, messaggio "Token scaduto"
    • TC-05.5: Conferma reset con token valido
        ◦ Input: token valido, nuova password="newpass123"
        ◦ Expected: Status 200, password aggiornata e hashata, token marcato isUsed=true, tutti i refresh token eliminati
    • TC-05.6: Conferma reset con token già usato
        ◦ Input: token con isUsed=true
        ◦ Expected: Status 400 Bad Request, messaggio "Token già utilizzato"
    • TC-05.7: Verifica rate limiting reset password
        ◦ Input: 6 richieste reset dallo stesso IP in 1 minuto
        ◦ Expected: Prime 5 OK, sesta Status 429 Too Many Requests


GESTIONE POST

RF-06: Creazione Post
Test Case Associati:
    • TC-06.1: Creazione post con solo testo
        ◦ Input: content="Questo è un test post", imageUrl=null
        ◦ Expected: Status 201, post creato con ID, contenuto salvato
    • TC-06.2: Creazione post con solo immagine
        ◦ Input: content=null, imageUrl="https://firebase.storage/image.jpg"
        ◦ Expected: Status 201, post creato con immagine URL
    • TC-06.3: Creazione post con testo e immagine
        ◦ Input: content="Post con immagine", imageUrl="https://firebase.storage/img.jpg"
        ◦ Expected: Status 201, post con entrambi i campi
    • TC-06.4: Creazione post senza contenuto né immagine
        ◦ Input: content=null, imageUrl=null
        ◦ Expected: Status 400 Bad Request, messaggio "Almeno contenuto o immagine richiesto"
    • TC-06.5: Creazione post con menzioni
        ◦ Input: content="Ciao @utente1 e @utente2", mentions=["utente1", "utente2"]
        ◦ Expected: Status 201, post creato, 2 notifiche MENTION generate
    • TC-06.6: Verifica rate limiting creazione post
        ◦ Input: 11 richieste creazione post in 1 minuto
        ◦ Expected: Prime 10 OK, undicesima Status 429 Too Many Requests

RF-07: Visualizzazione Feed Post
Test Case Associati:
    • TC-07.1: Feed post con paginazione
        ◦ Input: GET /api/posts?page=0&size=10
        ◦ Expected: Status 200, array di massimo 10 post, ordinati per data decrescente
    • TC-07.2: Feed esclude post eliminati
        ◦ Precondizione: Esistono post con isDeletedByAuthor=true
        ◦ Expected: Post eliminati non presenti nel risultato
    • TC-07.3: Feed esclude post nascosti dall'utente
        ◦ Precondizione: Utente ha nascosto alcuni post (hidden_posts)
        ◦ Expected: Post nascosti non presenti nel feed dell'utente
    • TC-07.4: Feed include flag likedByCurrentUser corretto
        ◦ Precondizione: Utente ha messo like ad alcuni post
        ◦ Expected: likedByCurrentUser=true per post con like, false altrimenti

RF-08: Visualizzazione Dettaglio Post
Test Case Associati:
    • TC-08.1: Dettaglio post esistente con commenti
        ◦ Input: GET /api/posts/{id}
        ◦ Expected: Status 200, dati completi post, lista commenti non eliminati
    • TC-08.2: Dettaglio post eliminato (non autore)
        ◦ Precondizione: Post con isDeletedByAuthor=true, utente non è autore
        ◦ Expected: Status 404 Not Found
    • TC-08.3: Dettaglio post eliminato (autore)
        ◦ Precondizione: Post eliminato, utente è autore
        ◦ Expected: Status 200, post visibile all'autore anche se eliminato

RF-09: Modifica Post
Test Case Associati:
    • TC-09.1: Modifica contenuto post proprio
        ◦ Input: PUT /api/posts/{id} con content="Contenuto modificato"
        ◦ Expected: Status 200, content aggiornato, updatedAt modificato
    • TC-09.2: Tentativo modifica post altrui
        ◦ Precondizione: Utente non è autore del post
        ◦ Expected: Status 403 Forbidden, messaggio "Non autorizzato"
    • TC-09.3: Modifica post con contenuto vuoto
        ◦ Input: content=""
        ◦ Expected: Status 400 Bad Request, errore validazione

RF-10: Eliminazione Post
Test Case Associati:
    • TC-10.1: Eliminazione post proprio
        ◦ Input: DELETE /api/posts/{id}
        ◦ Expected: Status 200, isDeletedByAuthor=true, menzioni eliminate, notifiche eliminate
    • TC-10.2: Eliminazione post altrui da admin
        ◦ Precondizione: Utente è ADMIN
        ◦ Expected: Status 200, post eliminato, azione registrata in audit log
    • TC-10.3: Tentativo eliminazione post altrui (non admin)
        ◦ Precondizione: Utente non è autore né admin
        ◦ Expected: Status 403 Forbidden


GESTIONE LIKE

RF-11: Like Post
Test Case Associati:
    • TC-11.1: Mettere like a post
        ◦ Precondizione: Utente non ha già messo like
        ◦ Expected: Status 200, like creato, likesCount incrementato, notifica LIKE generata
    • TC-11.2: Rimuovere like da post (toggle)
        ◦ Precondizione: Utente ha già messo like
        ◦ Expected: Status 200, like rimosso, likesCount decrementato
    • TC-11.3: Deduplicazione notifiche like
        ◦ Precondizione: Like/unlike/like entro 5 minuti
        ◦ Expected: Solo una notifica LIKE generata
    • TC-11.4: Verifica rate limiting like
        ◦ Input: 31 operazioni like/unlike in 1 minuto
        ◦ Expected: Prime 30 OK, trentunesima Status 429

RF-12: Visualizzazione Utenti Like
Test Case Associati:
    • TC-12.1: Lista utenti che hanno messo like
        ◦ Input: GET /api/posts/{id}/likes?page=0&size=20
        ◦ Expected: Status 200, lista utenti con username, nome, foto, data like


GESTIONE COMMENTI

RF-13: Creazione Commento
Test Case Associati:
    • TC-13.1: Commento con solo testo
        ◦ Input: content="Questo è un commento"
        ◦ Expected: Status 201, commento creato, commentsCount incrementato, notifica COMMENT generata
    • TC-13.2: Commento con menzioni
        ◦ Input: content="Ciao @user1", mentions=["user1"]
        ◦ Expected: Status 201, commento creato, notifiche COMMENT e MENTION generate
    • TC-13.3: Commento su post inesistente
        ◦ Input: postId non esistente
        ◦ Expected: Status 404 Not Found
    • TC-13.4: Verifica rate limiting commenti
        ◦ Input: 11 commenti in 1 minuto
        ◦ Expected: Prime 10 OK, undicesimo Status 429

RF-14: Modifica Commento
Test Case Associati:
    • TC-14.1: Modifica commento proprio
        ◦ Input: PUT /api/comments/{id} con content="Commento modificato"
        ◦ Expected: Status 200, content aggiornato, updatedAt modificato
    • TC-14.2: Tentativo modifica commento altrui
        ◦ Expected: Status 403 Forbidden

RF-15: Eliminazione Commento
Test Case Associati:
    • TC-15.1: Eliminazione commento proprio
        ◦ Expected: Status 200, isDeletedByAuthor=true, commentsCount decrementato
    • TC-15.2: Eliminazione commento altrui da admin
        ◦ Expected: Status 200, commento eliminato, audit log creato


PRIVACY E VISIBILITÀ

RF-16: Nascondere Post
Test Case Associati:
    • TC-16.1: Nascondere post dal proprio feed
        ◦ Input: POST /api/posts/{id}/hide
        ◦ Expected: Status 200, record in hidden_posts creato, post non appare più nel feed utente
    • TC-16.2: Mostrare post precedentemente nascosto
        ◦ Input: DELETE /api/posts/{id}/hide
        ◦ Expected: Status 200, record rimosso, post riappare nel feed

RF-17: Nascondere Commento
Test Case Associati:
    • TC-17.1: Nascondere commento
        ◦ Input: POST /api/comments/{id}/hide
        ◦ Expected: Status 200, record in hidden_comments creato
    • TC-17.2: Mostrare commento nascosto
        ◦ Input: DELETE /api/comments/{id}/hide
        ◦ Expected: Status 200, commento riappare


MESSAGGISTICA DIRETTA

RF-18: Invio Messaggio Diretto
Test Case Associati:
    • TC-18.1: Invio messaggio valido
        ◦ Input: recipientId=2, content="Ciao come stai?"
        ◦ Expected: Status 201, messaggio salvato con isRead=false, notifica DIRECT_MESSAGE, invio WebSocket
    • TC-18.2: Invio messaggio a destinatario inesistente
        ◦ Expected: Status 404 Not Found
    • TC-18.3: Invio messaggio a utente disattivato
        ◦ Expected: Status 400 Bad Request
    • TC-18.4: Verifica rate limiting messaggi
        ◦ Input: 21 messaggi in 1 minuto
        ◦ Expected: Prime 20 OK, ventunesimo Status 429

RF-19: Visualizzazione Conversazioni
Test Case Associati:
    • TC-19.1: Lista conversazioni con conteggio messaggi non letti
        ◦ Expected: Status 200, array conversazioni ordinate per data ultima attività, unreadCount corretto

RF-20: Visualizzazione Messaggi Conversazione
Test Case Associati:
    • TC-20.1: Messaggi con altro utente
        ◦ Input: GET /api/messages/conversation/{otherUserId}
        ◦ Expected: Status 200, lista messaggi ordinati cronologicamente, messaggi non letti marcati come letti

RF-21: Marcare Messaggio Come Letto
Test Case Associati:
    • TC-21.1: Marcare proprio messaggio ricevuto come letto
        ◦ Expected: Status 200, isRead=true, conferma via WebSocket al mittente


SISTEMA NOTIFICHE

RF-22: Visualizzazione Notifiche
Test Case Associati:
    • TC-22.1: Lista notifiche con paginazione
        ◦ Input: GET /api/notifications?page=0&size=20
        ◦ Expected: Status 200, notifiche ordinate per data decrescente
    • TC-22.2: Verifica tipi notifica presenti
        ◦ Expected: LIKE, COMMENT, MENTION, DIRECT_MESSAGE presenti correttamente

RF-23: Notifiche Real-time WebSocket
Test Case Associati:
    • TC-23.1: Connessione WebSocket con JWT valido
        ◦ Expected: Connessione stabilita, canali /user/queue/notifications e /user/queue/messages attivi
    • TC-23.2: Ricezione notifica real-time
        ◦ Precondizione: Utente connesso via WebSocket
        ◦ Azione: Altro utente mette like a un post dell'utente
        ◦ Expected: Notifica ricevuta istantaneamente via WebSocket

RF-24: Conteggio Notifiche Non Lette
Test Case Associati:
    • TC-24.1: Conteggio corretto notifiche non lette
        ◦ Expected: Status 200, contatore numerico corretto

RF-25: Segnare Notifiche Come Lette
Test Case Associati:
    • TC-25.1: Marcare singola notifica come letta
        ◦ Input: PUT /api/notifications/{id}/read
        ◦ Expected: Status 200, isRead=true per quella notifica
    • TC-25.2: Marcare tutte le notifiche come lette
        ◦ Input: PUT /api/notifications/read-all
        ◦ Expected: Status 200, tutte le notifiche con isRead=true


GESTIONE PROFILO

RF-26: Aggiornamento Profilo
Test Case Associati:
    • TC-26.1: Aggiornamento campi modificabili
        ◦ Input: fullName="Nuovo Nome", bio="La mia bio", profilePictureUrl="https://firebase.storage/avatar.jpg"
        ◦ Expected: Status 200, campi aggiornati, updatedAt modificato
    • TC-26.2: Tentativo modifica username (non permesso)
        ◦ Expected: Username rimane invariato
    • TC-26.3: Tentativo modifica email (non permesso)
        ◦ Expected: Email rimane invariata

RF-27: Visualizzazione Profilo Utente
Test Case Associati:
    • TC-27.1: Visualizzazione profilo pubblico
        ◦ Input: GET /api/users/{username}
        ◦ Expected: Status 200, dati profilo, conteggio post, email visibile solo al proprietario
    • TC-27.2: Visualizzazione profilo utente inesistente
        ◦ Expected: Status 404 Not Found

RF-28: Disattivazione Proprio Account
Test Case Associati:
    • TC-28.1: Disattivazione con password corretta
        ◦ Input: password corrente
        ◦ Expected: Status 200, isActive=false, impossibilità login
    • TC-28.2: Disattivazione con password errata
        ◦ Expected: Status 401 Unauthorized

RF-29: Ricerca Utenti
Test Case Associati:
    • TC-29.1: Ricerca per username
        ◦ Input: GET /api/users/search?q=pippo
        ◦ Expected: Status 200, utenti il cui username contiene "pippo"
    • TC-29.2: Ricerca per nome completo
        ◦ Input: GET /api/users/search?q=Mario
        ◦ Expected: Status 200, utenti il cui fullName contiene "Mario"
    • TC-29.3: Ricerca esclude utenti disattivati
        ◦ Expected: Solo utenti con isActive=true nei risultati


SISTEMA MENZIONI

RF-30: Menzioni
Test Case Associati:
    • TC-30.1: Estrazione menzioni da post
        ◦ Input: content="Ciao @user1 e @user2 come va?"
        ◦ Expected: Menzioni estratte, notifiche MENTION generate per user1 e user2
    • TC-30.2: Menzioni in commento
        ◦ Expected: Menzioni salvate, notifiche generate

RF-31: Visualizzazione Menzioni Utente
Test Case Associati:
    • TC-31.1: Lista proprie menzioni
        ◦ Input: GET /api/mentions
        ◦ Expected: Status 200, lista menzioni ordinate per data


FUNZIONALITÀ AMMINISTRATIVE

RF-32: [ADMIN] Eliminazione Post Altri Utenti
Test Case Associati:
    • TC-32.1: Admin elimina post utente
        ◦ Precondizione: Utente autenticato come ADMIN
        ◦ Expected: Status 200, post eliminato, audit log creato

RF-33: [ADMIN] Eliminazione Commenti Altri Utenti
Test Case Associati:
    • TC-33.1: Admin elimina commento utente
        ◦ Expected: Status 200, commento eliminato, audit log creato

RF-34: [ADMIN] Disattivazione/Riattivazione Utente
Test Case Associati:
    • TC-34.1: Admin disattiva utente
        ◦ Expected: Status 200, isActive=false, audit log creato
    • TC-34.2: Admin riattiva utente disattivato
        ◦ Expected: Status 200, isActive=true, audit log creato

RF-35: [ADMIN] Visualizzazione Statistiche
Test Case Associati:
    • TC-35.1: Statistiche generali sistema
        ◦ Expected: Status 200, totali utenti/post/commenti/like/messaggi corretti
    • TC-35.2: Statistiche ultimi 7 giorni
        ◦ Expected: Conteggi filtrati per data ultimi 7 giorni

RF-36: [ADMIN] Visualizzazione Audit Log
Test Case Associati:
    • TC-36.1: Lista audit log con paginazione
        ◦ Expected: Status 200, log ordinati per data decrescente, dettagli completi azioni

RF-37: [ADMIN] Visualizzazione Lista Utenti
Test Case Associati:
    • TC-37.1: Lista tutti gli utenti
        ◦ Expected: Status 200, tutti gli utenti con filtri stato

RF-38: [ADMIN] Gestione Rate Limiting
Test Case Associati:
    • TC-38.1: Visualizzazione statistiche cache
        ◦ Expected: Status 200, dimensione cache, hit rate, eviction count
    • TC-38.2: Reset bucket per utente specifico
        ◦ Expected: Status 200, bucket rate limit resettato


TASK AUTOMATICI

RF-39: Pulizia Automatica
Test Case Associati:
    • TC-39.1: Verifica esecuzione task schedulato pulizia token
        ◦ Expected: Task eseguito alle 04:30, token scaduti eliminati
    • TC-39.2: Verifica eliminazione refresh token scaduti
        ◦ Expected: Refresh token con expiry < now eliminati
    • TC-39.3: Verifica eliminazione session inattive
        ◦ Expected: User sessions con lastActivity > 30 giorni eliminate


REPORT DI COLLAUDO


Risultati effettivi dei Test Case

Esempio formato risultati:

TC-01.1: Registrazione con dati validi
Eseguito: Sì
Data esecuzione: 8 dicembre 2025
Input utilizzato:
    • username: "pippo"
    • email: "pippo@marconirovereto.it"
    • password: "pluto123"
    • fullName: "Pippo Test"
Risultato atteso: Status 201, access token JWT generato, refresh token generato, utente creato nel database
Risultato ottenuto: PASS
    • Status: 201 Created
    • Access token ricevuto: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    • Refresh token ricevuto: 550e8400-e29b-41d4-a716-446655440000
    • Utente ID: 42
    • Password hashata con BCrypt verificata nel database
Esito: PASS ✓

TC-02.1: Login con credenziali valide
Eseguito: Sì
Data esecuzione: 8 dicembre 2025
Input utilizzato:
    • username: "pippo"
    • password: "pluto"
Risultato atteso: Status 200, access token JWT, refresh token, dati utente, lastSeen aggiornato
Risultato ottenuto: PASS
    • Status: 200 OK
    • Access token valido ricevuto
    • Refresh token valido ricevuto
    • Campo lastSeen aggiornato a timestamp corrente
    • Dati utente completi nel response body
Esito: PASS ✓


BUG E RISOLUZIONI


Elenco dei problemi maggiori incontrati durante il collaudo e come sono stati risolti

Bug #1: Refresh token non persisteva dopo restart backend
Severità: CRITICA
Descrizione: Dopo il riavvio del backend, tutti i refresh token diventavano "non valido o scaduto" anche se creati pochi minuti prima.
Riproduzione:
    1. Login utente → refresh token generato
    2. Restart backend (mvn spring-boot:run)
    3. Tentativo refresh token → errore "Token non valido o scaduto"
Causa root: Configurazione Hibernate ddl-auto: create-drop eliminava tutte le tabelle (inclusi refresh_tokens) ad ogni riavvio.
Soluzione implementata:
    • Cambiato ddl-auto da create-drop a update in application.yml
    • Verificato che refresh_tokens persistano tra restart
    • Test: Login → restart → refresh token ancora funzionante ✓
Data risoluzione: 7 dicembre 2025
Commit: fix: cambio ddl-auto da create-drop a update per persistenza dati

Bug #2: Dipendenza circolare NotificationService ↔ WebSocketController
Severità: MEDIA
Descrizione: Spring lanciava errore "Circular dependency" all'avvio perché NotificationService richiedeva WebSocketController e viceversa.
Causa root: NotificationService doveva inviare notifiche via WebSocket chiamando direttamente il controller.
Soluzione implementata:
    • Refactoring ad architettura event-driven
    • NotificationService pubblica NotificationCreatedEvent
    • Creato NotificationWebSocketListener che ascolta evento e invia via WebSocket
    • Eliminata dipendenza diretta tra service e controller
    • Vantaggio aggiuntivo: invio asincrono con @Async
Data risoluzione: 5 dicembre 2025
Commit: refactor: risolto circular dependency con pattern event-driven

Bug #3: Service Worker non cachava correttamente le API in produzione
Severità: BASSA
Descrizione: In build production Angular Service Worker non cachava le chiamate API come configurato in ngsw-config.json.
Causa root: URL API in dataGroups non matchavano perché mancava wildcard corretta.
Soluzione implementata:
    • Aggiornato ngsw-config.json con pattern "https://**/api/**"
    • Testato caching offline funzionante
Data risoluzione: 6 dicembre 2025

Bug #4: PWA manifest icon dimensions mismatch
Severità: BASSA
Descrizione: Browser console mostrava warning "Manifest icon size mismatch" perché manifest dichiarava dimensioni diverse dai file effettivi.
Causa root: Manifest dichiarava 128x128 ma file era 144x144.
Soluzione implementata:
    • Aggiornato manifest.webmanifest con dimensioni corrette
    • Riutilizzato file più vicini per dimensioni mancanti
Data risoluzione: 7 dicembre 2025


ISTRUZIONI DI MESSA IN ESERCIZIO


Guida passo-passo per l'installazione e l'avvio del progetto su un ambiente di produzione


PREREQUISITI

Software richiesto:
    • Java JDK 25 o superiore
    • Maven 3.9+
    • Node.js 20+ e npm 10+
    • PostgreSQL 17
    • Account Firebase (per hosting frontend e storage immagini)
    • Account Fly.io (per deploy backend)
    • Account Cloudinary (per storage immagini opzionale)
    • Account Gmail con App Password (per invio email)

Account e configurazioni esterne:
    • Firebase Project con Hosting e Storage abilitati
    • Fly.io account con CLI installato
    • PostgreSQL database su Fly.io (gestito direttamente, no Docker)
    • Gmail App Password per SMTP
    • Cloudinary API keys (opzionale)


CONFIGURAZIONE BACKEND

Passo 1: Clone repository
git clone https://github.com/mousti0113/class_social_media.git
cd class_social_media/backend

Passo 2: Configurazione variabili d'ambiente
Creare file .env nella directory backend/ con i seguenti valori:

# Database PostgreSQL (Fly.io managed)
JDBC_DATABASE_URL=jdbc:postgresql://<fly-db-host>:5432/class_social_db
DB_USERNAME=<db-username>
DB_PASSWORD=<db-password>
DDL_AUTO=update

# JWT Security
JWT_SECRET=<genera-stringa-random-64-caratteri>
JWT_ACCESS_EXPIRATION=1800000

# Email SMTP (Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=<tuo-email@gmail.com>
MAIL_PASSWORD=<gmail-app-password>

# Cloudinary (opzionale)
CLOUDINARY_CLOUD_NAME=<cloud-name>
CLOUDINARY_API_KEY=<api-key>
CLOUDINARY_API_SECRET=<api-secret>

# CORS (URL frontend Firebase)
ALLOWED_ORIGINS=https://<tuo-progetto>.web.app,https://<tuo-progetto>.firebaseapp.com

# Logging
SHOW_SQL=false

IMPORTANTE: Non committare mai il file .env su Git (già in .gitignore)

Passo 3: Verifica Dockerfile backend
Il Dockerfile è già configurato per build multi-stage con Maven e JRE 25.
Nessuna modifica necessaria.

Passo 4: Deploy backend su Fly.io

# Login Fly.io
fly auth login

# Crea app Fly.io
fly launch --name class-social-backend --region fra

# Configura database PostgreSQL managed su Fly.io
fly postgres create --name class-social-db --region fra

# Collega database all'app
fly postgres attach class-social-db --app class-social-backend

# Imposta secrets (variabili d'ambiente)
fly secrets set JWT_SECRET=<secret> MAIL_PASSWORD=<password> CLOUDINARY_API_SECRET=<secret>

# Deploy applicazione
fly deploy

# Verifica status
fly status
fly logs

URL backend: https://class-social-backend.fly.dev


CONFIGURAZIONE FRONTEND

Passo 1: Installazione dipendenze
cd ../frontend
npm install

Passo 2: Configurazione Firebase
# Installa Firebase CLI
npm install -g firebase-tools

# Login Firebase
firebase login

# Inizializza progetto
firebase init hosting

Selezionare:
    • Build directory: dist/frontend/browser
    • Single Page App: Yes
    • Overwrite index.html: No

Passo 3: Configurazione environment production
Modificare src/environments/environment.prod.ts:

export const environment = {
  production: true,
  apiUrl: 'https://class-social-backend.fly.dev/api',
  wsUrl: 'wss://class-social-backend.fly.dev/ws',
  cloudinary: {
    cloudName: '<tuo-cloud-name>',
    uploadPreset: '<upload-preset>'
  }
};

Passo 4: Build e deploy frontend
# Build production
npm run build -- --configuration production

# Deploy su Firebase Hosting
firebase deploy --only hosting

URL frontend: https://<tuo-progetto>.web.app


CONFIGURAZIONE DATABASE

Il database PostgreSQL su Fly.io è già configurato automaticamente con fly postgres attach.

Verifica connessione:
fly postgres connect --app class-social-db

Lo schema viene creato automaticamente da Hibernate all'avvio (ddl-auto: update).


TEST POST-DEPLOYMENT

Passo 1: Verifica backend API
curl https://class-social-backend.fly.dev/actuator/health

Expected output:
{"status":"UP"}

Passo 2: Test registrazione utente
POST https://class-social-backend.fly.dev/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@marconirovereto.it",
  "password": "test123",
  "nomeCompleto": "Test User"
}

Expected: Status 201, access token e refresh token ricevuti

Passo 3: Test login
POST https://class-social-backend.fly.dev/api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "test123"
}

Expected: Status 200, token JWT validi

Passo 4: Verifica frontend Firebase
Aprire https://<tuo-progetto>.web.app nel browser
Expected:
    • Applicazione carica correttamente
    • Pagina login/registrazione visibile
    • PWA installabile (icona + nella barra indirizzi)
    • Service Worker attivo (verificare in DevTools → Application → Service Workers)

Passo 5: Test end-to-end
    1. Registrazione nuovo utente dal frontend
    2. Verifica ricezione email benvenuto
    3. Login
    4. Creazione post con testo e immagine (upload Firebase)
    5. Like/commento ad altro post
    6. Verifica notifiche real-time (WebSocket)
    7. Test PWA offline (disattiva rete, app deve funzionare con cache)


MANUTENZIONE E MONITORING

Monitoring Fly.io Backend:
# Logs in tempo reale
fly logs --app class-social-backend

# Metriche
fly dashboard class-social-backend

# Restart se necessario
fly apps restart class-social-backend

Monitoring Firebase Frontend:
# Console Firebase
firebase open hosting

# Analytics traffico e performance

Database Backup:
# Backup automatico Fly.io Postgres (retention 7 giorni)
fly postgres backup list --app class-social-db

# Restore da backup
fly postgres restore --app class-social-db --backup-id <id>

Scaling:
# Backend Fly.io (se necessario)
fly scale count 2 --app class-social-backend
fly scale memory 1024 --app class-social-backend

# Frontend Firebase Hosting scala automaticamente


MANUALE UTENTE


Istruzioni chiare e semplici per l'utente finale su come utilizzare le funzionalità principali


REGISTRAZIONE E ACCESSO

Come registrarsi:
    1. Aprire l'applicazione nel browser: https://<tuo-progetto>.web.app
    2. Cliccare su "Registrati" nella pagina iniziale
    3. Compilare il form:
        ◦ Username: scegliere un username univoco (lettere, numeri, underscore)
        ◦ Email: inserire email scolastica @marconirovereto.it
        ◦ Password: minimo 6 caratteri
        ◦ Nome completo: il tuo nome e cognome
    4. Cliccare "Registrati"
    5. Riceverai una email di benvenuto
    6. Accesso automatico alla piattaforma

Come fare il login:
    1. Nella pagina iniziale, inserire username e password
    2. Cliccare "Accedi"
    3. Verrai reindirizzato al feed principale

Password dimenticata:
    1. Cliccare "Password dimenticata?" nella pagina login
    2. Inserire la tua email @marconirovereto.it
    3. Riceverai una email con link per reset password
    4. Cliccare il link (valido 1 ora)
    5. Inserire nuova password
    6. Confermare → puoi fare login con la nuova password


CREAZIONE E GESTIONE POST

Come creare un post:
    1. Dal feed principale, cliccare il pulsante "+" o "Nuovo Post"
    2. Scrivere il contenuto del post (max 5000 caratteri)
    3. (Opzionale) Cliccare icona immagine per caricare una foto
    4. (Opzionale) Menzionare altri utenti scrivendo @username
    5. Cliccare "Pubblica"
    6. Il post apparirà nel feed

Come modificare un post:
    1. Nel tuo post, cliccare icona "..." (tre puntini)
    2. Selezionare "Modifica"
    3. Modificare il testo (l'immagine non può essere modificata)
    4. Cliccare "Salva modifiche"

Come eliminare un post:
    1. Nel tuo post, cliccare icona "..."
    2. Selezionare "Elimina"
    3. Confermare eliminazione
    4. Il post sarà rimosso dal feed

Come nascondere post di altri:
    1. In un post che non vuoi vedere, cliccare "..."
    2. Selezionare "Nascondi post"
    3. Il post sparirà dal tuo feed (gli altri lo vedranno ancora)


INTERAZIONI SOCIALI

Come mettere like a un post:
    1. Cliccare l'icona cuore sotto un post
    2. Il cuore diventa rosso = like messo
    3. L'autore riceverà una notifica
    4. Cliccare di nuovo per rimuovere il like

Come commentare un post:
    1. Cliccare "Commenta" sotto un post
    2. Scrivere il commento (max 2000 caratteri)
    3. (Opzionale) Menzionare utenti con @username
    4. Cliccare "Pubblica"
    5. L'autore riceverà una notifica

Come modificare/eliminare un commento:
    • Modifica: Cliccare "..." sul tuo commento → "Modifica"
    • Elimina: Cliccare "..." → "Elimina"

Come menzionare qualcuno:
    1. Nei post o commenti, scrivere @ seguito da username (es. @mario)
    2. Apparirà un suggerimento automatico
    3. Selezionare l'utente
    4. L'utente menzionato riceverà una notifica


MESSAGGI DIRETTI

Come inviare un messaggio privato:
    1. Cliccare icona "Messaggi" nel menu
    2. Cliccare "Nuova conversazione" o selezionare conversazione esistente
    3. Cercare l'utente per username
    4. Scrivere il messaggio (max 5000 caratteri)
    5. Cliccare "Invia"
    6. L'utente riceverà una notifica real-time se online

Come visualizzare i messaggi:
    1. Vai alla sezione "Messaggi"
    2. Le conversazioni sono ordinate dalla più recente
    3. I messaggi non letti sono evidenziati
    4. Cliccare su una conversazione per aprirla
    5. I messaggi vengono marcati automaticamente come letti


NOTIFICHE

Tipi di notifiche:
    • 💙 Like: qualcuno ha messo like a un tuo post
    • 💬 Commento: qualcuno ha commentato il tuo post
    • @ Menzione: sei stato menzionato in un post o commento
    • ✉️ Messaggio: hai ricevuto un messaggio privato

Come visualizzare le notifiche:
    1. Cliccare icona campanella nel menu in alto
    2. Vedrai lista notifiche ordinate per data
    3. Cliccare su una notifica per andare al contenuto relativo
    4. Le notifiche vengono marcate automaticamente come lette

Come attivare notifiche real-time:
    • Le notifiche appaiono automaticamente in tempo reale quando qualcuno interagisce con i tuoi contenuti
    • Nessuna configurazione necessaria, funziona automaticamente


PROFILO UTENTE

Come modificare il profilo:
    1. Cliccare sulla tua foto profilo in alto a destra
    2. Selezionare "Impostazioni"
    3. Modificare:
        ◦ Nome completo
        ◦ Bio (breve descrizione di te, max 100 caratteri)
        ◦ Foto profilo (carica nuova immagine)
    4. Cliccare "Salva modifiche"
    
    Nota: Username e email NON possono essere modificati

Come visualizzare profilo di altri utenti:
    1. Cliccare su username o foto profilo di un utente
    2. Vedrai:
        ◦ Info profilo (nome, bio, foto)
        ◦ Tutti i post pubblicati dall'utente
        ◦ Data registrazione
        ◦ Ultimo accesso

Come cercare altri utenti:
    1. Cliccare icona lente di ingrandimento
    2. Scrivere username o nome dell'utente
    3. Apparirà lista risultati
    4. Cliccare su un utente per vedere il profilo


FUNZIONALITÀ PWA (INSTALLAZIONE APP)

Come installare l'app sul dispositivo:

Su desktop (Chrome/Edge):
    1. Aprire l'applicazione nel browser
    2. Cliccare icona "+" nella barra indirizzi
    3. Cliccare "Installa"
    4. L'app apparirà come applicazione desktop

Su smartphone (Android):
    1. Aprire l'app nel browser Chrome
    2. Toccare menu (tre puntini)
    3. Selezionare "Aggiungi a schermata Home"
    4. L'app apparirà come icona nella home

Su iPhone (iOS):
    1. Aprire l'app in Safari
    2. Toccare icona condividi (quadrato con freccia)
    3. Selezionare "Aggiungi alla schermata Home"

Uso offline:
    • L'app funziona anche senza connessione internet
    • Puoi visualizzare post e contenuti già caricati
    • Le azioni (like, commenti) verranno sincronizzate quando torni online


FUNZIONALITÀ AMMINISTRATIVE (SOLO ADMIN)

Come eliminare post/commenti inappropriati:
    1. Individuare post o commento da rimuovere
    2. Cliccare "..." → "Elimina (Admin)"
    3. Confermare eliminazione
    4. L'azione viene registrata nel log audit

Come disattivare/riattivare un utente:
    1. Dashboard Admin → Lista Utenti
    2. Cercare l'utente
    3. Cliccare "Disattiva" o "Riattiva"
    4. Confermare
    5. Utente disattivato non potrà più fare login

Come visualizzare statistiche:
    1. Dashboard Admin → Statistiche
    2. Vedrai:
        ◦ Totale utenti registrati
        ◦ Utenti attivi
        ◦ Post, commenti, like totali
        ◦ Statistiche ultimi 7 giorni

Come gestire rate limiting:
    1. Dashboard Admin → Rate Limiting
    2. Visualizza statistiche cache
    3. Puoi resettare bucket per utente/IP specifico se necessario
