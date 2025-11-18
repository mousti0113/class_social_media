import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token-service';

/**
 * Interceptor per aggiungere automaticamente il token JWT alle richieste HTTP
 * 
 * Logica:
 * 1. Verifica se la richiesta necessita di autenticazione
 * 2. Se sì, recupera il token e lo aggiunge all'header Authorization
 * 3. Le richieste agli endpoint pubblici vengono ignorate
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);

  // Endpoint pubblici che non richiedono autenticazione
  const publicEndpoints = [
    '/auth/login',
    '/auth/register',
    '/auth/reset-password',
    '/auth/confirm-reset-password',
    '/auth/validate-reset-token'
  ];

  // Verifica se l'URL è pubblico
  const isPublicEndpoint = publicEndpoints.some(endpoint => 
    req.url.includes(endpoint)
  );

  // Se è un endpoint pubblico, procedi senza modifiche
  if (isPublicEndpoint) {
    return next(req);
  }

  // Recupera il token di accesso
  const token = tokenService.getAccessToken();

  // Se non c'è token, procede comunque (l'error interceptor gestirà il 401)
  if (!token) {
    return next(req);
  }

  // Clona la richiesta e aggiungi l'header Authorization
  const clonedRequest = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  // Procedi con la richiesta modificata
  return next(clonedRequest);
};