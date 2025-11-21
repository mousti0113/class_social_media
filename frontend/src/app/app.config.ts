import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/auth/interceptors/auth.interceptor';
import { errorInterceptor } from './core/auth/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    // Configurazione HTTP client con interceptors
    // // 1. authInterceptor viene eseguito PER PRIMO (aggiunge il token)
    // 2. errorInterceptor viene eseguito PER SECONDO (gestisce errori e refresh)
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
  ],
};
