# 🚀 Guida Completa al Deployment

## Panoramica

- **Frontend**: Firebase Hosting (Angular PWA)
- **Backend**: Fly.io (Spring Boot + PostgreSQL)
- **Storage Immagini**: Cloudinary (già configurato)
- **Database**: PostgreSQL su Fly.io

---

## 📋 Prerequisiti

### 1. Installa gli strumenti necessari

```bash
# Firebase CLI
npm install -g firebase-tools

# Fly.io CLI
# Windows (PowerShell):
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"

# Verifica installazioni
firebase --version
flyctl version
```

### 2. Account necessari

- ✅ Account Firebase (https://console.firebase.google.com)
- ✅ Account Fly.io (https://fly.io/app/sign-up)
- ✅ Account Cloudinary (già configurato)

---

## 🔧 PARTE 1: PREPARAZIONE BACKEND (Fly.io)

### Step 1.1: Login su Fly.io

```bash
# Login (aprirà browser per autenticazione)
flyctl auth login
```

### Step 1.2: Crea file fly.toml nella root del backend

**File**: `backend/fly.toml`

```toml
# fly.toml - Configurazione Fly.io per Spring Boot Backend
app = "class-social-media-backend"  # Cambia con un nome unico
primary_region = "ams"  # Amsterdam (o "fra" per Frankfurt)

[build]
  dockerfile = "Dockerfile"

[env]
  SPRING_PROFILE = "production"
  DDL_AUTO = "update"  # IMPORTANTE: NON usare create-drop in produzione
  SHOW_SQL = "false"
  LOG_LEVEL = "WARN"
  APP_LOG_LEVEL = "INFO"
  MAX_STUDENTS = "17"

[http_service]
  internal_port = 8080
  force_https = true
  auto_stop_machines = false  # Mantieni sempre attivo
  auto_start_machines = true
  min_machines_running = 1
  processes = ["app"]

  [http_service.concurrency]
    type = "requests"
    soft_limit = 200
    hard_limit = 250

[[vm]]
  cpu_kind = "shared"
  cpus = 1
  memory_mb = 512  # 512MB gratuiti con Fly.io

[mounts]
  source = "uploads_data"
  destination = "/app/uploads"
  initial_size = "1GB"

[[services]]
  protocol = "tcp"
  internal_port = 8080

  [[services.ports]]
    port = 80
    handlers = ["http"]
    force_https = true

  [[services.ports]]
    port = 443
    handlers = ["tls", "http"]

  [[services.tcp_checks]]
    interval = "15s"
    timeout = "10s"
    grace_period = "30s"

  [[services.http_checks]]
    interval = "30s"
    timeout = "10s"
    grace_period = "30s"
    method = "get"
    path = "/actuator/health"
    protocol = "http"
```

### Step 1.3: Crea l'app su Fly.io

```bash
cd backend

# Crea l'app (usa il nome che hai messo in fly.toml)
flyctl apps create class-social-media-backend

# Crea un database PostgreSQL
flyctl postgres create --name class-social-media-db --region ams

# Connetti il database all'app
flyctl postgres attach class-social-media-db --app class-social-media-backend
```

**Importante**: Fly.io creerà automaticamente la variabile `DATABASE_URL` con la connection string PostgreSQL.

### Step 1.4: Configura i Secret del Backend

```bash
# JWT Secret (GENERA UNA STRINGA RANDOM SICURA!)
# Puoi generarla con: openssl rand -base64 32
flyctl secrets set JWT_SECRET="TUO_JWT_SECRET_SICURO_QUI" --app class-social-media-backend

# CORS - URL del frontend Firebase (lo avrai dopo deploy frontend)
flyctl secrets set CORS_ALLOWED_ORIGINS="https://class-social-media.web.app,https://class-social-media.firebaseapp.com" --app class-social-media-backend

# Cloudinary
flyctl secrets set CLOUDINARY_CLOUD_NAME="duenbvoog" --app class-social-media-backend
flyctl secrets set CLOUDINARY_API_KEY="TUA_CLOUDINARY_API_KEY" --app class-social-media-backend
flyctl secrets set CLOUDINARY_API_SECRET="TUA_CLOUDINARY_API_SECRET" --app class-social-media-backend

# Email (se usi Gmail per esempio)
flyctl secrets set MAIL_HOST="smtp.gmail.com" --app class-social-media-backend
flyctl secrets set MAIL_PORT="587" --app class-social-media-backend
flyctl secrets set MAIL_USERNAME="tua-email@gmail.com" --app class-social-media-backend
flyctl secrets set MAIL_PASSWORD="tua-app-password-gmail" --app class-social-media-backend

# Spring Security Admin
flyctl secrets set SPRING_SECURITY_USER_NAME="admin" --app class-social-media-backend
flyctl secrets set SPRING_SECURITY_USER_PASSWORD="PASSWORD_ADMIN_SICURA" --app class-social-media-backend

# Verifica i secret configurati (mostra solo i nomi, non i valori)
flyctl secrets list --app class-social-media-backend
```

### Step 1.5: Modifica application.yml per produzione

**File**: `backend/src/main/resources/application.yml`

Assicurati che usi le variabili d'ambiente:

```yaml
spring:
  datasource:
    # Fly.io fornisce DATABASE_URL, dobbiamo mapparlo a JDBC_DATABASE_URL
    url: ${DATABASE_URL:${JDBC_DATABASE_URL:jdbc:postgresql://localhost:5432/beetUs_db}}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      # ⚠️ CRITICAL: In produzione usa "update" NON "create-drop"
      ddl-auto: ${DDL_AUTO:update}
```

**Oppure**, crea un profilo separato production:

**File**: `backend/src/main/resources/application-production.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}  # Fly.io fornisce questo
  jpa:
    hibernate:
      ddl-auto: update  # Mai create-drop in produzione!
    show-sql: false
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 1800000  # 30 minuti
  refresh-token-expiration: 2592000000  # 30 giorni

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}

logging:
  level:
    root: WARN
    com.example.backend: INFO
```

### Step 1.6: Deploy del Backend

```bash
cd backend

# Deploy (prima volta)
flyctl deploy

# Fly.io farà:
# 1. Build Docker image usando il Dockerfile
# 2. Push dell'immagine
# 3. Deploy dell'app con i secret configurati

# Verifica lo status
flyctl status --app class-social-media-backend

# Vedi i logs
flyctl logs --app class-social-media-backend

# Apri l'app nel browser
flyctl open --app class-social-media-backend
```

**URL Backend**: `https://class-social-media-backend.fly.dev`

### Step 1.7: Testa il Backend

```bash
# Health check
curl https://class-social-media-backend.fly.dev/actuator/health

# Dovrebbe rispondere:
# {"status":"UP"}
```

---

## 🎨 PARTE 2: PREPARAZIONE FRONTEND (Firebase)

### Step 2.1: Login su Firebase

```bash
# Login Firebase
firebase login

# Seleziona il tuo account Google
```

### Step 2.2: Inizializza Firebase nel progetto

```bash
cd frontend

# Inizializza Firebase
firebase init

# Seleziona (usa SPAZIO per selezionare, INVIO per confermare):
# ✅ Hosting: Configure files for Firebase Hosting
# ❌ Altre opzioni (non servono per ora)

# Domande:
# ? Please select an option: Use an existing project
# ? Select a default Firebase project: [Crea o seleziona il tuo progetto]
# ? What do you want to use as your public directory? dist/frontend/browser
# ? Configure as a single-page app (rewrite all urls to /index.html)? Yes
# ? Set up automatic builds and deploys with GitHub? No
# ? File dist/frontend/browser/index.html already exists. Overwrite? No
```

### Step 2.3: Aggiorna environment.prod.ts

**File**: `frontend/src/environments/environment.prod.ts`

```typescript
import { Environment } from './environment';

export const environment: Environment = {
  production: true,

  // Backend API URL - URL di Fly.io
  apiUrl: 'https://class-social-media-backend.fly.dev/api',

  // WebSocket URL - URL di Fly.io
  wsUrl: 'https://class-social-media-backend.fly.dev/ws',

  // Cloudinary (stesso di dev)
  cloudinary: {
    cloudName: 'duenbvoog',
    uploadPreset: 'classconnect_images',
    folder: 'classconnect',
  },

  // Resto configurazione (uguale a dev)
  uploadMaxSize: 5 * 1024 * 1024,
  imageMaxWidth: 1920,
  imageMaxHeight: 1920,
  allowedImageTypes: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
  defaultPageSize: 20,
  maxPageSize: 100,
  searchDebounceMs: 300,
  typingIndicatorDebounceMs: 500,
  wsReconnectInterval: 5000,
  wsMaxReconnectAttempts: 10,
  toastDuration: 3000,
  toastPosition: 'top-right' as const,
  httpCacheTimeout: 5 * 60 * 1000,
};
```

### Step 2.4: Crea firebase.json (se non esiste)

**File**: `frontend/firebase.json`

```json
{
  "hosting": {
    "public": "dist/frontend/browser",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "destination": "/index.html"
      }
    ],
    "headers": [
      {
        "source": "**/*.@(jpg|jpeg|gif|png|svg|webp|ico)",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "max-age=31536000"
          }
        ]
      },
      {
        "source": "**/*.@(js|css)",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "max-age=31536000"
          }
        ]
      },
      {
        "source": "manifest.webmanifest",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "max-age=0"
          }
        ]
      },
      {
        "source": "ngsw.json",
        "headers": [
          {
            "key": "Cache-Control",
            "value": "max-age=0"
          }
        ]
      }
    ]
  }
}
```

### Step 2.5: Build del Frontend per Produzione

```bash
cd frontend

# Build produzione con PWA
npm run build

# Verifica che sia stata creata la cartella dist/frontend/browser
ls dist/frontend/browser
```

### Step 2.6: Deploy del Frontend

```bash
# Deploy su Firebase
firebase deploy --only hosting

# Output:
# ✔  Deploy complete!
#
# Project Console: https://console.firebase.google.com/project/your-project/overview
# Hosting URL: https://your-project.web.app
```

**URL Frontend**: `https://class-social-media.web.app` (o il tuo project ID)

### Step 2.7: Aggiorna CORS sul Backend

Ora che hai l'URL di Firebase, aggiorna il CORS sul backend:

```bash
# Sostituisci con il TUO URL di Firebase
flyctl secrets set CORS_ALLOWED_ORIGINS="https://class-social-media.web.app,https://class-social-media.firebaseapp.com" --app class-social-media-backend

# Rideploy backend per applicare il cambio
cd backend
flyctl deploy
```

---

## 🔐 PARTE 3: GESTIONE SECRET E SICUREZZA

### 3.1: Secret Backend (Fly.io)

**I secret su Fly.io sono variabili d'ambiente criptate**. Sono gestiti con:

```bash
# Lista secret
flyctl secrets list --app class-social-media-backend

# Aggiungi/aggiorna secret
flyctl secrets set SECRET_NAME="valore" --app class-social-media-backend

# Rimuovi secret
flyctl secrets unset SECRET_NAME --app class-social-media-backend

# Import da file .env (utile!)
flyctl secrets import --app class-social-media-backend < secrets.env
```

**Esempio file `secrets.env`** (NON committare su Git!):

```env
JWT_SECRET=tuo_jwt_secret_sicuro_qui
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=tuo_secret_cloudinary
MAIL_USERNAME=tua-email@gmail.com
MAIL_PASSWORD=tua-password-app
CORS_ALLOWED_ORIGINS=https://class-social-media.web.app,https://class-social-media.firebaseapp.com
SPRING_SECURITY_USER_PASSWORD=admin_password_sicura
```

**Importa tutti i secret in un colpo solo**:

```bash
flyctl secrets import --app class-social-media-backend < secrets.env
```

### 3.2: Secret Frontend (Firebase)

**Firebase Hosting NON ha secret lato client** perché è solo hosting statico. I secret sono gestiti nel backend.

**Environment variables nel frontend sono HARDCODED nel build**:
- `environment.prod.ts` contiene URL pubblici (backend API, Cloudinary cloud name)
- NON mettere API keys o secret sensibili nel frontend!
- Cloudinary `uploadPreset` è configurato come "unsigned upload" (sicuro)

### 3.3: Checklist Sicurezza

**Backend**:
- ✅ JWT_SECRET deve essere random e lungo (min 32 caratteri)
- ✅ DDL_AUTO deve essere "update" o "validate" (mai "create-drop")
- ✅ CORS configurato solo con URL del frontend (no wildcard "*")
- ✅ HTTPS forzato su Fly.io (force_https = true)
- ✅ Password admin Spring Security sicura
- ✅ Mail password usa "App Password" se Gmail (non password principale)

**Frontend**:
- ✅ apiUrl e wsUrl puntano a HTTPS (Fly.io)
- ✅ PWA configurato correttamente (manifest.webmanifest)
- ✅ Service Worker attivo (ngsw-config.json)

**Database**:
- ✅ PostgreSQL su Fly.io con connessione criptata
- ✅ Backup automatici (Fly.io PostgreSQL li fa di default)

---

## 📝 PARTE 4: FILE .gitignore

**Assicurati che questi file NON siano committati su Git**:

**Root `.gitignore`**:
```
# Secret files
secrets.env
.env
.env.local
.env.production

# Firebase
.firebase/
.firebaserc
firebase-debug.log

# Fly.io
fly.toml.local
```

**Backend `.gitignore`**:
```
# Spring Boot
target/
*.log

# Secret
application-production.yml
secrets.env
```

**Frontend `.gitignore`**:
```
# Build
dist/
.angular/

# Firebase
.firebase/
```

---

## 🚀 PARTE 5: WORKFLOW DI DEPLOYMENT

### Deploy Iniziale (fatto sopra)

1. Backend: `flyctl deploy` dal folder `backend/`
2. Frontend: `firebase deploy --only hosting` dal folder `frontend/`

### Deploy Successivi

**Ogni volta che fai modifiche**:

```bash
# Backend (Spring Boot)
cd backend
git pull  # Se lavori in team
mvn clean package -DskipTests  # Test locale
flyctl deploy

# Frontend (Angular)
cd frontend
git pull  # Se lavori in team
npm run build  # Build produzione
firebase deploy --only hosting
```

### Rollback (se qualcosa va storto)

**Backend (Fly.io)**:
```bash
# Vedi le release precedenti
flyctl releases --app class-social-media-backend

# Rollback all'ultima versione stabile
flyctl releases rollback --app class-social-media-backend
```

**Frontend (Firebase)**:
```bash
# Vedi le versioni precedenti nella Firebase Console
# Hosting > Release History > Seleziona versione > Restore
```

---

## 🔍 PARTE 6: MONITORAGGIO E DEBUG

### Backend (Fly.io)

```bash
# Logs in tempo reale
flyctl logs --app class-social-media-backend

# Status dell'app
flyctl status --app class-social-media-backend

# Connettiti al database
flyctl postgres connect --app class-social-media-db

# SSH nell'app (per debug)
flyctl ssh console --app class-social-media-backend

# Metriche
flyctl dashboard --app class-social-media-backend
```

### Frontend (Firebase)

```bash
# Apri Firebase Console
firebase open hosting

# Oppure vai su:
# https://console.firebase.google.com/project/TUO_PROJECT/hosting
```

**Firebase Console ti mostra**:
- Traffic (requests al secondo)
- Bandwidth usage
- Release history
- Errori (se integri Firebase Analytics)

---

## 🎯 PARTE 7: COSTI E LIMITI

### Fly.io (Backend + Database)

**Piano Gratuito Include**:
- ✅ 3 VM shared-cpu-1x con 256MB RAM (noi usiamo 1 VM da 512MB = metà quota gratuita)
- ✅ 3GB volume storage persistente
- ✅ 160GB bandwidth/mese
- ✅ PostgreSQL database (3GB storage)

**Se superi**:
- $0.02/ora per VM aggiuntive
- $0.15/GB storage/mese

**Stima per questo progetto**: Gratis o ~$5/mese se hai traffico medio

### Firebase (Frontend)

**Piano Spark (Gratuito) Include**:
- ✅ 10GB storage
- ✅ 360MB/giorno bandwidth (~10GB/mese)
- ✅ Hosting illimitato

**Se superi**: Passa a piano Blaze (pay-as-you-go)
- $0.026/GB storage
- $0.15/GB bandwidth

**Stima per questo progetto**: Gratis per traffico basso/medio

### Cloudinary (Storage Immagini)

**Piano Gratuito**:
- ✅ 25 credits/mese
- ✅ ~25GB storage + bandwidth

**Stima per questo progetto**: Gratis se moderate upload immagini

### Totale Stimato

**Con traffico basso/medio**: **$0-5/mese** ✅

---

## 📚 PARTE 8: RISORSE UTILI

### Documentazione

- **Fly.io Docs**: https://fly.io/docs/
- **Firebase Docs**: https://firebase.google.com/docs/hosting
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

### CLI Commands Quick Reference

```bash
# Fly.io
flyctl auth login
flyctl apps list
flyctl status --app APP_NAME
flyctl logs --app APP_NAME
flyctl secrets list --app APP_NAME
flyctl deploy
flyctl scale count 1 --app APP_NAME  # Scala VM
flyctl scale memory 512 --app APP_NAME  # Scala RAM

# Firebase
firebase login
firebase projects:list
firebase deploy --only hosting
firebase hosting:channel:deploy CHANNEL_NAME  # Deploy preview
```

---

## ✅ CHECKLIST FINALE PRE-DEPLOY

### Backend Checklist

- [ ] `fly.toml` configurato con nome app corretto
- [ ] `application.yml` usa variabili d'ambiente
- [ ] Tutti i secret configurati su Fly.io (`flyctl secrets list`)
- [ ] `DDL_AUTO=update` (NON create-drop)
- [ ] Dockerfile funzionante (`docker build -t test .`)
- [ ] Health check endpoint `/actuator/health` attivo
- [ ] CORS configurato con URL frontend Firebase

### Frontend Checklist

- [ ] `environment.prod.ts` aggiornato con URL backend Fly.io
- [ ] `firebase.json` configurato correttamente
- [ ] Build produzione funzionante (`npm run build`)
- [ ] PWA configurato (manifest + service worker)
- [ ] Firebase project creato e inizializzato

### Secret Checklist

- [ ] JWT_SECRET generato (random, 32+ caratteri)
- [ ] Cloudinary API key/secret configurati
- [ ] Mail credentials configurati (app password se Gmail)
- [ ] CORS_ALLOWED_ORIGINS con URL Firebase
- [ ] Spring Security admin password sicura
- [ ] File `secrets.env` in `.gitignore`

### Test Checklist

- [ ] Backend health check: `curl https://APP.fly.dev/actuator/health`
- [ ] Frontend carica: `https://APP.web.app`
- [ ] Login funziona
- [ ] Upload immagini funziona (Cloudinary)
- [ ] WebSocket connesso (vedi console browser)
- [ ] Email invio funziona (test registrazione)

---

## 🆘 TROUBLESHOOTING COMUNI

### Errore: "CORS policy blocked"

**Causa**: CORS_ALLOWED_ORIGINS non include URL frontend

**Fix**:
```bash
flyctl secrets set CORS_ALLOWED_ORIGINS="https://TUO_APP.web.app,https://TUO_APP.firebaseapp.com" --app APP_NAME
flyctl deploy
```

### Errore: "WebSocket connection failed"

**Causa**: Backend non raggiungibile o wsUrl sbagliato

**Fix**:
1. Verifica `environment.prod.ts` ha `wsUrl: 'https://APP.fly.dev/ws'`
2. Verifica backend running: `flyctl status`
3. Controlla logs: `flyctl logs`

### Errore: "Database connection failed"

**Causa**: DATABASE_URL non configurato o database offline

**Fix**:
```bash
# Verifica database status
flyctl postgres list

# Verifica connection string
flyctl postgres db list --app class-social-media-db

# Re-attach database
flyctl postgres attach class-social-media-db --app class-social-media-backend
```

### Errore: "JWT secret must not be default"

**Causa**: JWT_SECRET non configurato o ancora "default-secret"

**Fix**:
```bash
# Genera secret sicuro
openssl rand -base64 32

# Configura
flyctl secrets set JWT_SECRET="OUTPUT_DEL_COMANDO_SOPRA" --app APP_NAME
```

### Build Frontend fallisce

**Causa**: Errori TypeScript o dipendenze mancanti

**Fix**:
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run build
```

---

## 🎉 CONGRATULAZIONI!

Se hai seguito tutti gli step, ora hai:

✅ Backend Spring Boot su Fly.io con PostgreSQL
✅ Frontend Angular PWA su Firebase
✅ Storage immagini su Cloudinary
✅ HTTPS su entrambi frontend e backend
✅ Secret gestiti in modo sicuro
✅ WebSocket funzionante
✅ Database persistente con backup automatici

**Prossimi step opzionali**:
- Configura dominio custom (Firebase + Fly.io supportano domini custom)
- Configura CI/CD (GitHub Actions per deploy automatici)
- Aggiungi Firebase Analytics per monitorare utenti
- Configura alerting su Fly.io (email quando app down)

---

**Link Utili Finali**:
- Fly.io Dashboard: https://fly.io/dashboard
- Firebase Console: https://console.firebase.google.com
- Cloudinary Dashboard: https://console.cloudinary.com

**Buon deploy! 🚀**
