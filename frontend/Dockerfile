# Dockerfile per Angular Frontend
# Build stage
FROM node:24-alpine AS build

WORKDIR /app

# Copia package.json e package-lock.json
COPY package*.json ./

# Installa dipendenze
RUN npm ci 

# Copia il codice sorgente
COPY . .

# Build dell'applicazione Angular
RUN npm run build

# Runtime stage con Nginx
FROM nginx:alpine

# Rimuovi configurazione default di nginx
RUN rm -rf /usr/share/nginx/html/*

# Copia i file buildati da Angular
COPY  --from=build /app/dist/frontend/browser /usr/share/nginx/html

# Copia configurazione nginx personalizzata
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Esponi porta 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:80 || exit 1

# Avvia nginx
CMD ["nginx", "-g", "daemon off;"]