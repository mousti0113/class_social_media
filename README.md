# beetUs - Social Media per Studenti

Social network dedicato agli studenti dell'istituto Marconi Rovereto con funzionalità di post, commenti, like, messaggi diretti, notifiche real-time e sistema di menzioni.

## 🚀 Deployment

- **Frontend**: [https://beetus-frontend.vercel.app](https://beetus-frontend.vercel.app) (Vercel)
- **Backend**: [https://beetus-frontend.onrender.com](https://beetus-frontend.onrender.com) (Render.com)
- **Database**: PostgreSQL su Render.com (free tier)

## 🛠 Tecnologie

### Backend
- Java 25 + Spring Boot 3.5.6
- PostgreSQL 17
- JWT Authentication (access + refresh token)
- WebSocket (STOMP) per real-time
- Cloudinary per storage immagini
- Docker multi-stage builds
- Bucket4j per rate limiting

### Frontend
- Angular 20 (Standalone Components + Zoneless)
- TypeScript 5.9
- Tailwind CSS 4
- Signals per state management
- PWA con Service Worker
- Infinite scroll

## 📦 Setup Locale

### Prerequisiti
- Java JDK 25+
- Maven 3.9+
- Node.js 20+ e npm 10+
- PostgreSQL 17

### Backend
```bash
cd backend
# Crea file .env con configurazioni (vedi DOCUMENTAZIONE.md)
mvn clean install
mvn spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
# Apri http://localhost:4200
```

## 📚 Documentazione

- **[DOCUMENTAZIONE.md](DOCUMENTAZIONE.md)** - Architettura completa, tecnologie, setup
- **[PIANO_DI_COLLAUDO.md](PIANO_DI_COLLAUDO.md)** - Test case, risultati, bug risolti

## 🔐 Funzionalità

- ✅ Registrazione con verifica email (@marconirovereto.it)
- ✅ Login con JWT (access + refresh token auto-refresh)
- ✅ Creazione post (testo + immagine Cloudinary)
- ✅ Like, commenti con menzioni (@username)
- ✅ Messaggi diretti real-time (WebSocket)
- ✅ Notifiche push real-time
- ✅ Ricerca utenti
- ✅ Profilo personalizzabile
- ✅ Dashboard admin (statistiche, gestione utenti)
- ✅ PWA installabile (iOS/Android/Desktop)
- ✅ Rate limiting per protezione API

## 👨‍💻 Sviluppatore

Moustapha Mbaye - [@mousti0113](https://github.com/mousti0113)