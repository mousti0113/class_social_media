import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth-service';
import { TokenService } from '../services/token-service';

/**
 * Interceptor per gestire errori HTTP e refresh token automatico
 * 
 * Gestisce:
 * - 401 Unauthorized: tenta refresh token e riprova la richiesta
 * - 403 Forbidden: redirect o notifica
 * - 500+ Server errors: logging e notifica utente
 * 
 * Logica refresh token:
 * 1. Se riceviamo 401 e non è la chiamata di refresh
 * 2. Proviamo a refreshare il token
 * 3. Se il refresh ha successo, riproviamo la richiesta originale
 * 4. Se il refresh fallisce, effettuiamo logout
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Gestisce errori 401 Unauthorized
      if (error.status === 401) {
        return handle401Error(req, next, authService, tokenService);
      }

      // Gestisce errori 403 Forbidden
      if (error.status === 403) {
        return handle403Error(error);
      }

      // Gestisce errori 5xx Server Error
      if (error.status >= 500) {
        return handle5xxError(error);
      }

      // Per tutti gli altri errori, propagali
      return throwError(() => error);
    })
  );
};

/**
 * Gestisce errore 401 con tentativo di refresh token
 */
function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
  tokenService: TokenService
) {
  // Non tentare refresh se siamo già sulla chiamata di refresh
  // o su endpoint pubblici
  if (
    req.url.includes('/auth/refresh-token') ||
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register')
  ) {
    // Effettua logout se refresh token invalido
    if (req.url.includes('/auth/refresh-token')) {
      authService.logout().subscribe();
    }
    return throwError(() => new Error('Token non valido'));
  }

  // Tenta il refresh del token
  return authService.refreshToken().pipe(
    switchMap(() => {
      // Refresh riuscito, riprova la richiesta originale con nuovo token
      const token = tokenService.getAccessToken();
      
      const clonedRequest = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      return next(clonedRequest);
    }),
    catchError((refreshError) => {
      // Refresh fallito, effettua logout
      authService.logout().subscribe();
      
      console.error('Errore refresh token:', refreshError);
      return throwError(() => new Error('Sessione scaduta. Effettua nuovamente il login.'));
    })
  );
}
/**
 * Gestisce errore 403 Forbidden
 */
function handle403Error(error: HttpErrorResponse) {
  console.error('Accesso negato:', error.message);
  
  // Qui potresti mostrare un toast o redirect
  // esempio: toastService.show('Non hai i permessi per questa operazione', 'error');
  
  return throwError(() => new Error('Non hai i permessi per eseguire questa operazione.'));
}

/**
 * Gestisce errori 5xx Server Error
 */
function handle5xxError(error: HttpErrorResponse) {
  console.error('Errore server:', error);
  
  // Qui potresti mostrare un toast o notifica generica
  // esempio: toastService.show('Errore del server. Riprova più tardi.', 'error');
  
  return throwError(() => new Error('Errore del server. Riprova più tardi.'));
}