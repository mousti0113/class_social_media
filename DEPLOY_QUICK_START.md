# 🚀 Quick Start: Deploy in 15 Minuti

Guida rapida per fare il deploy dell'applicazione. Per dettagli completi vedi [GUIDA_DEPLOYMENT.md](GUIDA_DEPLOYMENT.md).

---

## Step 1: Installa CLI Tools

```bash
# Firebase CLI
npm install -g firebase-tools

# Fly.io CLI (Windows PowerShell)
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"
```

---

## Step 2: Backend su Fly.io

```bash
# 1. Login
flyctl auth login

# 2. Vai nella cartella backend
cd backend

# 3. Crea l'app (cambia il nome se necessario in fly.toml prima!)
flyctl apps create class-social-media-backend

# 4. Crea database PostgreSQL
flyctl postgres create --name class-social-media-db --region ams

# 5. Connetti database all'app
flyctl postgres attach class-social-media-db --app class-social-media-backend

# 6. Configura i secret
# Copia secrets.env.example in secrets.env e compila con i tuoi valori
cp secrets.env.example secrets.env
# Modifica secrets.env con i tuoi valori reali

# 7. Importa tutti i secret
flyctl secrets import --app class-social-media-backend < secrets.env

# 8. Deploy!
flyctl deploy
```

**URL Backend**: `https://class-social-media-backend.fly.dev`

---

## Step 3: Frontend su Firebase

```bash
# 1. Login Firebase
firebase login

# 2. Vai nella cartella frontend
cd frontend

# 3. Inizializza Firebase
firebase init
# Seleziona: Hosting
# Public directory: dist/frontend/browser
# Single-page app: Yes
# Overwrite index.html: No

# 4. Aggiorna environment.prod.ts
# Modifica apiUrl e wsUrl con l'URL del backend Fly.io:
# apiUrl: 'https://class-social-media-backend.fly.dev/api'
# wsUrl: 'https://class-social-media-backend.fly.dev/ws'

# 5. Build produzione
npm run build

# 6. Deploy!
firebase deploy --only hosting
```

**URL Frontend**: `https://class-social-media.web.app` (o il tuo project ID)

---

## Step 4: Aggiorna CORS

```bash
# Sostituisci con il TUO URL Firebase
flyctl secrets set CORS_ALLOWED_ORIGINS="https://class-social-media.web.app,https://class-social-media.firebaseapp.com" --app class-social-media-backend

# Rideploy backend
cd backend
flyctl deploy
```

---

## ✅ Verifica Funzionamento

```bash
# Backend health check
curl https://class-social-media-backend.fly.dev/actuator/health

# Frontend - apri nel browser
https://class-social-media.web.app
```

---

## 🔄 Deploy Successivi

**Backend**:
```bash
cd backend
flyctl deploy
```

**Frontend**:
```bash
cd frontend
npm run build
firebase deploy --only hosting
```

---

## 📝 Secret Necessari

In `backend/secrets.env`:

```env
JWT_SECRET=genera_con_openssl_rand_base64_32
CLOUDINARY_CLOUD_NAME=duenbvoog
CLOUDINARY_API_KEY=tua_api_key
CLOUDINARY_API_SECRET=tuo_secret
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tua-email@gmail.com
MAIL_PASSWORD=tua-app-password
CORS_ALLOWED_ORIGINS=https://tua-app.web.app,https://tua-app.firebaseapp.com
SPRING_SECURITY_USER_PASSWORD=password_admin_sicura
```

**Genera JWT Secret**:
```bash
openssl rand -base64 32
```

---

## 🆘 Problemi Comuni

**CORS blocked**: Verifica che CORS_ALLOWED_ORIGINS includa l'URL Firebase corretto

**WebSocket failed**: Verifica che wsUrl in environment.prod.ts usi HTTPS

**Database error**: Verifica che il database sia attached: `flyctl postgres list`

**Build failed**: Pulisci e rebuilda:
```bash
rm -rf node_modules package-lock.json
npm install
npm run build
```

---

Per la guida completa con troubleshooting avanzato, vedi [GUIDA_DEPLOYMENT.md](GUIDA_DEPLOYMENT.md).
