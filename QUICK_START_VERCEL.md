# ⚡ Quick Start: Deploy con Vercel in 10 Minuti

Guida ultra-rapida per deploy backend (Fly.io) + frontend (Vercel).

---

## Step 1: Backend su Fly.io (5 minuti)

```bash
# 1. Installa Fly CLI (Windows)
powershell -Command "iwr https://fly.io/install.ps1 -useb | iex"

# 2. Login
flyctl auth login

# 3. Vai nel backend e crea app
cd backend
flyctl apps create beetUs-backend

# 4. Crea database e connettilo
flyctl postgres create --name beetus-db --region ams
flyctl postgres attach beetus-db --app beetUs-backend

# 5. Configura secret
# Crea file secrets.env (copia da secrets.env.example e compila)
# Poi importa:
flyctl secrets import --app beetUs-backend < secrets.env

# 6. Deploy!
flyctl deploy --app beetUs-backend

# 7. Test
curl https://beetus-backend.fly.dev/actuator/health
```

---

## Step 2: Frontend su Vercel (5 minuti)

### Opzione A: Deploy da UI (Più Facile) ✅

1. **Push progetto su GitHub**:
   ```bash
   git add .
   git commit -m "Ready for deploy"
   git push
   ```

2. **Vai su Vercel**:
   - Apri https://vercel.com/signup
   - Registrati con GitHub (gratis, no carta)
   - Click "Add New..." → "Project"
   - Seleziona il tuo repository

3. **Configura** (schermata "Configure Project"):
   
   **IMPORTANTE**: Cerca la sezione **"Root Directory"**:
   - Clicca su **"Edit"** accanto a "Root Directory"
   - Seleziona `frontend` dalla lista
   - Clicca "Continue"
   
   **Vercel auto-rileva Angular** e imposta automaticamente:
   - ✅ Framework Preset: Angular
   - ✅ Build Command: `npm run build`
   - ✅ Output Directory: `dist/frontend/browser`
   
   Se vuoi modificare, clicca **"Override"** nella sezione "Build and Output Settings"

4. **Deploy**: Click "Deploy" → Fatto! 🎉
   
   ⏱️ Il primo deploy richiede 2-3 minuti

### Opzione B: Deploy da CLI

```bash
# 1. Installa Vercel CLI
npm install -g vercel

# 2. Login
vercel login

# 3. Deploy (dalla cartella frontend)
cd frontend
vercel --prod
```

---

## Step 3: Aggiorna CORS (1 minuto)

```bash
# Sostituisci con il TUO URL Vercel (es: beetus-frontend.vercel.app)
flyctl secrets set CORS_ALLOWED_ORIGINS="https://TUO-URL.vercel.app" --app beetUs-backend

# Rideploy backend
cd backend
flyctl deploy --app beetUs-backend
```

---

## ✅ Test Finale

1. **Backend**: `curl https://beetus-backend.fly.dev/actuator/health`
2. **Frontend**: Apri `https://TUO-URL.vercel.app`
3. **Test completo**: Registrati, login, crea post

---

## 🔐 Secret Necessari

In `backend/secrets.env`:

```env
JWT_SECRET=genera_con_openssl_rand_base64_32
CLOUDINARY_CLOUD_NAME=duenbvoog
CLOUDINARY_API_KEY=tua_key
CLOUDINARY_API_SECRET=tuo_secret
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tua-email@gmail.com
MAIL_PASSWORD=tua-app-password
CORS_ALLOWED_ORIGINS=https://TUO-URL.vercel.app
SPRING_SECURITY_USER_PASSWORD=password_admin
```

**Genera JWT**:
```bash
openssl rand -base64 32
```

---

## 🔄 Deploy Successivi

**Frontend (Automatico)**:
```bash
git add .
git commit -m "Update"
git push
# Vercel fa deploy automatico!
```

**Backend (Manuale)**:
```bash
cd backend
flyctl deploy --app beetUs-backend
```

---

Per la guida completa vedi [DEPLOY_VERCEL_GUIDE.md](DEPLOY_VERCEL_GUIDE.md)
