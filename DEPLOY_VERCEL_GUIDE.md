# 🚀 Guida Deploy con Vercel (Frontend) + Fly.io (Backend)

**Setup Aggiornato**: Frontend su Vercel invece di Firebase (nessuna carta richiesta!)

---

## ✅ Vantaggi di Vercel

- **Completamente gratuito** senza carta di credito
- **Deploy automatici** da GitHub (push → deploy automatico)
- **HTTPS** incluso con certificato SSL
- **CDN globale** ultra veloce
- **Zero configurazione** necessaria
- **PWA support** completo
- **Preview deployments** per ogni PR

---

## 📋 PARTE 1: BACKEND SU FLY.IO

### Step 1.1: Installa Fly.io CLI

```bash
# Windows PowerShell
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"

# Verifica installazione
flyctl version
```

### Step 1.2: Login e Setup Database

```bash
# Login
flyctl auth login

# Vai nella cartella backend
cd backend

# Crea l'app (nome già configurato: beetUs-backend in fly.toml)
flyctl apps create beetUs-backend

# Crea database PostgreSQL
flyctl postgres create --name beetus-db --region ams

# Connetti database all'app
flyctl postgres attach beetus-db --app beetUs-backend
```

### Step 1.3: Configura i Secret

Crea file `backend/secrets.env` (copia da `secrets.env.example`):

```env
# JWT Secret - Genera con: openssl rand -base64 32
JWT_SECRET=TUO_JWT_SECRET_QUI_MIN_32_CARATTERI

# Cloudinary
CLOUDINARY_CLOUD_NAME=duenbvoog
CLOUDINARY_API_KEY=tua_cloudinary_api_key
CLOUDINARY_API_SECRET=tua_cloudinary_api_secret

# Email (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tua-email@gmail.com
MAIL_PASSWORD=tua-app-password-gmail

# CORS - Aggiorneremo dopo deploy Vercel
CORS_ALLOWED_ORIGINS=http://localhost:4200

# Spring Security Admin
SPRING_SECURITY_USER_NAME=admin
SPRING_SECURITY_USER_PASSWORD=tua_password_admin_sicura
```

**Genera JWT Secret**:
```bash
openssl rand -base64 32
```

**Importa i secret**:
```bash
cd backend
flyctl secrets import --app beetUs-backend < secrets.env
```

### Step 1.4: Deploy Backend

```bash
cd backend
flyctl deploy --app beetUs-backend
```

**Verifica funzionamento**:
```bash
# Health check
curl https://beetus-backend.fly.dev/actuator/health
```

**Output atteso**: `{"status":"UP"}`

✅ **URL Backend**: `https://beetus-backend.fly.dev`

---

## 🎨 PARTE 2: FRONTEND SU VERCEL

### Step 2.1: Prepara il Progetto

**Aggiorna environment.prod.ts** con URL backend:

```typescript
// frontend/src/environments/environment.prod.ts
export const environment: Environment = {
  production: true,

  // ✅ URL Backend Fly.io
  apiUrl: 'https://beetus-backend.fly.dev/api',
  wsUrl: 'https://beetus-backend.fly.dev/ws',

  // Resto configurazione (uguale)
  cloudinary: {
    cloudName: 'duenbvoog',
    uploadPreset: 'classconnect_images',
    folder: 'classconnect',
  },
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

### Step 2.2: Push su GitHub

Se non hai ancora pushato il progetto su GitHub:

```bash
# Nella root del progetto
git init
git add .
git commit -m "Initial commit"

# Crea repository su GitHub, poi:
git remote add origin https://github.com/TUO_USERNAME/TUO_REPO.git
git branch -M main
git push -u origin main
```

### Step 2.3: Deploy su Vercel

**Opzione A: Deploy da UI (Consigliato - più facile)**

1. Vai su https://vercel.com/signup
2. Registrati con GitHub (gratuito, no carta richiesta)
3. Click "Add New..." → "Project"
4. Importa il tuo repository GitHub
5. **Configurazione progetto**:
   - **Framework Preset**: Seleziona "Angular"
   - **Root Directory**: Lascia vuoto (o clicca "Edit" e seleziona `frontend`)
   - **Build Command**: `npm run build` (già preconfigurato)
   - **Output Directory**: `dist/frontend/browser`
   - **Install Command**: `npm install`
6. Click "Deploy"

**Opzione B: Deploy da CLI**

```bash
# Installa Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy (dalla cartella frontend)
cd frontend
vercel

# Segui il wizard:
# ? Set up and deploy "frontend"? Yes
# ? Which scope? [Il tuo account]
# ? Link to existing project? No
# ? What's your project's name? beetus-frontend
# ? In which directory is your code located? ./
# ? Want to override the settings? No

# Deploy produzione
vercel --prod
```

✅ **URL Frontend**: `https://beetus-frontend.vercel.app` (o il nome che hai scelto)

### Step 2.4: Aggiorna CORS sul Backend

Ora che hai l'URL Vercel, aggiorna CORS:

```bash
# Sostituisci con il TUO URL Vercel
flyctl secrets set CORS_ALLOWED_ORIGINS="https://beetus-frontend.vercel.app" --app beetUs-backend

# Rideploy backend
cd backend
flyctl deploy --app beetUs-backend
```

---

## 🔄 Deploy Automatici (Bonus)

**Con Vercel + GitHub**, ogni volta che fai push su GitHub:
- ✅ Vercel fa automaticamente il build e deploy del frontend
- ✅ Ogni branch ha una preview URL
- ✅ Solo il branch `main` va in produzione

**Per il backend su Fly.io**:

```bash
# Deploy manuale quando modifichi il backend
cd backend
flyctl deploy --app beetUs-backend
```

**Opzione: GitHub Actions per auto-deploy backend** (avanzato):

Crea `.github/workflows/deploy-backend.yml`:

```yaml
name: Deploy Backend to Fly.io

on:
  push:
    branches:
      - main
    paths:
      - 'backend/**'

jobs:
  deploy:
    name: Deploy Backend
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: superfly/flyctl-actions/setup-flyctl@master
      - name: Deploy to Fly.io
        run: flyctl deploy --remote-only --app beetUs-backend
        working-directory: backend
        env:
          FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
```

Per configurarlo:
1. Vai su https://fly.io/user/personal_access_tokens
2. Crea un token
3. Aggiungilo come secret su GitHub (Settings → Secrets → Actions → `FLY_API_TOKEN`)

---

## 🔐 GESTIONE SECRET

### Backend (Fly.io)

```bash
# Lista secret
flyctl secrets list --app beetUs-backend

# Aggiungi/Aggiorna
flyctl secrets set SECRET_NAME="valore" --app beetUs-backend

# Rimuovi
flyctl secrets unset SECRET_NAME --app beetUs-backend

# Import da file
flyctl secrets import --app beetUs-backend < secrets.env
```

### Frontend (Vercel)

**Vercel non ha secret lato client** perché è hosting statico.

Tutti i valori in `environment.prod.ts` sono pubblici (compilati nel bundle JavaScript).

**Solo URL pubblici** nel frontend:
- ✅ API URL backend (https://beetus-backend.fly.dev/api)
- ✅ Cloudinary cloud name (pubblico)
- ❌ NO API keys sensibili nel frontend!

---

## ✅ VERIFICA FUNZIONAMENTO

### 1. Backend Health Check

```bash
curl https://beetus-backend.fly.dev/actuator/health
```

**Output atteso**: `{"status":"UP"}`

### 2. Frontend Caricato

Apri nel browser: `https://beetus-frontend.vercel.app`

### 3. Test Completo

1. Registra un nuovo utente
2. Login
3. Crea un post con immagine (test Cloudinary)
4. Verifica WebSocket (commenti/like in tempo reale)
5. Test PWA (installa app da browser mobile)

---

## 📊 COSTI

| Servizio | Piano | Costo |
|----------|-------|-------|
| **Fly.io** (Backend + DB) | Free tier | $0 (o ~$5/mese se superi limiti) |
| **Vercel** (Frontend) | Hobby | $0 (100GB bandwidth/mese gratis) |
| **Cloudinary** (Immagini) | Free tier | $0 (25 credits/mese) |
| **TOTALE** | | **$0-5/mese** ✅ |

---

## 🔄 WORKFLOW DEPLOY SUCCESSIVI

### Frontend (Automatico con Git)

```bash
# Fai modifiche al frontend
cd frontend
# ... fai le tue modifiche ...

# Push su GitHub
git add .
git commit -m "Update frontend"
git push

# ✅ Vercel fa deploy automaticamente!
```

### Backend (Manuale)

```bash
# Fai modifiche al backend
cd backend
# ... fai le tue modifiche ...

# Test locale
mvn clean package -DskipTests

# Deploy su Fly.io
flyctl deploy --app beetUs-backend
```

---

## 🆘 TROUBLESHOOTING

### ❌ CORS Blocked

**Problema**: Frontend non riesce a chiamare backend

**Causa**: CORS_ALLOWED_ORIGINS non include URL Vercel

**Fix**:
```bash
# Verifica URL corretto su Vercel (Settings → Domains)
flyctl secrets set CORS_ALLOWED_ORIGINS="https://TUO-URL.vercel.app" --app beetUs-backend
flyctl deploy --app beetUs-backend
```

### ❌ WebSocket Connection Failed

**Problema**: Chat/notifiche real-time non funzionano

**Causa**: wsUrl non configurato correttamente

**Fix**: Verifica in `frontend/src/environments/environment.prod.ts`:
```typescript
wsUrl: 'https://beetus-backend.fly.dev/ws',  // ✅ Deve essere HTTPS
```

Rebuilda e redeploy:
```bash
cd frontend
npm run build
git add .
git commit -m "Fix WebSocket URL"
git push  # Vercel fa deploy automatico
```

### ❌ Build Failed su Vercel

**Problema**: Vercel non riesce a buildare

**Causa**: Errori TypeScript o dipendenze mancanti

**Fix**:
```bash
# Test build locale
cd frontend
npm run build

# Se funziona locale ma non su Vercel, verifica:
# 1. Node version in package.json:
{
  "engines": {
    "node": ">=18.x"
  }
}

# 2. Vercel settings → Build & Development Settings:
#    - Build Command: npm run build
#    - Output Directory: dist/frontend/browser
```

### ❌ Database Connection Failed

**Problema**: Backend non si connette al database

**Fix**:
```bash
# Verifica database status
flyctl postgres list

# Verifica attachment
flyctl postgres db list --app beetus-db

# Re-attach se necessario
flyctl postgres attach beetus-db --app beetUs-backend
```

### ❌ JWT Error "default-secret"

**Problema**: Backend rifiuta richieste con errore JWT

**Causa**: JWT_SECRET non configurato

**Fix**:
```bash
# Genera secret
openssl rand -base64 32

# Configura
flyctl secrets set JWT_SECRET="OUTPUT_DEL_COMANDO_SOPRA" --app beetUs-backend
```

---

## 📱 PWA TESTING

### Desktop (Chrome)

1. Apri `https://beetus-frontend.vercel.app`
2. Icona "Installa" nella barra degli indirizzi
3. Click "Installa" → App installata!

### Mobile (Android)

1. Apri in Chrome: `https://beetus-frontend.vercel.app`
2. Menu (⋮) → "Aggiungi a schermata Home"
3. App si apre in fullscreen come app native!

### iOS (Safari)

1. Apri in Safari: `https://beetus-frontend.vercel.app`
2. Tap "Condividi" → "Aggiungi a Home"
3. App installata!

---

## 🎯 CHECKLIST FINALE

### Pre-Deploy

- [x] Backend: `fly.toml` con app name corretto (`beetUs-backend`)
- [x] Backend: `secrets.env` compilato con valori reali
- [x] Backend: JWT_SECRET generato (min 32 caratteri)
- [x] Backend: Cloudinary credentials configurati
- [x] Backend: Email credentials configurati
- [x] Frontend: `environment.prod.ts` con URL Fly.io
- [x] Frontend: `vercel.json` configurato
- [x] Frontend: Progetto pushato su GitHub

### Post-Deploy

- [ ] Backend health check funziona
- [ ] Frontend carica correttamente
- [ ] Login funziona
- [ ] Registrazione + email funziona
- [ ] Upload immagini funziona (Cloudinary)
- [ ] WebSocket funziona (test commenti/like real-time)
- [ ] PWA installabile (test su mobile)
- [ ] CORS configurato con URL Vercel

---

## 🌟 RISORSE UTILI

- **Vercel Dashboard**: https://vercel.com/dashboard
- **Fly.io Dashboard**: https://fly.io/dashboard
- **Vercel Docs**: https://vercel.com/docs
- **Fly.io Docs**: https://fly.io/docs/

---

## 🎉 Deploy Completato!

Ora hai:
- ✅ Backend Spring Boot su Fly.io (PostgreSQL incluso)
- ✅ Frontend Angular PWA su Vercel
- ✅ Deploy automatici da Git (solo frontend)
- ✅ HTTPS su tutto
- ✅ Tutto GRATIS (o ~$5/mese max)

**Prossimi step opzionali**:
- Custom domain su Vercel (gratuito!)
- GitHub Actions per auto-deploy backend
- Monitoring con Vercel Analytics (gratuito)
- Error tracking con Sentry

**Buon deploy! 🚀**
