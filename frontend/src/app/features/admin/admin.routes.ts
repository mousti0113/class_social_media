import { Routes } from '@angular/router';
import { adminGuard } from '../../core/auth/guards/admin-guard';

/**
 * Routes per la sezione amministrazione
 *
 * Tutte le route sono protette da adminGuard che verifica:
 * - L'utente sia autenticato
 * - L'utente abbia il ruolo di amministratore
 */
export const adminRoutes: Routes = [
  {
    path: '',
    canActivate: [adminGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./dashboard/dashboard-component/dashboard-component').then(
            (m) => m.DashboardComponent
          ),
        title: 'Dashboard Admin - beetUs',
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./users/users-component/users-component').then(
            (m) => m.UsersComponent
          ),
        title: 'Gestione Utenti - beetUs',
      },
      {
        path: 'moderation',
        loadComponent: () =>
          import('./moderation/moderation-component/moderation-component').then(
            (m) => m.ModerationComponent
          ),
        title: 'Moderazione - beetUs',
      },
    ],
  },
];
