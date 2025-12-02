import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth-guard';

export const homeRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feed/feed-component/feed-component').then((m) => m.FeedComponent),
    title: 'Home - beetUs',
    canActivate: [authGuard],
  },
];
