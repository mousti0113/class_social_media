import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth-guard';
import { MainLayoutComponent } from '../../layout/main-layout/main-layout-component/main-layout-component';

export const homeRoutes: Routes = [
  // Messaggi - Full screen senza header (stile WhatsApp)
  {
    path: 'messages',
    canActivate: [authGuard],
    loadComponent: () =>
      import('../messages/messages-layout/messages-layout-component/messages-layout-component').then((m) => m.MessagesLayoutComponent),
    title: 'Messaggi - beetUs',
    children: [
      {
        path: ':userId',
        loadComponent: () =>
          import('../messages/chat/chat-component/chat-component').then((m) => m.ChatComponent),
        title: 'Chat - beetUs',
      },
    ],
  },
  // Layout principale con header
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./feed/feed-component/feed-component').then((m) => m.FeedComponent),
        title: 'Home - beetUs',
      },
      {
        path: 'post/:id',
        loadComponent: () =>
          import('../post/post-detail/post-detail-component/post-detail-component').then((m) => m.PostDetailComponent),
        title: 'Post - beetUs',
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('../notifications/notifications-list-component/notifications-list-component').then((m) => m.NotificationsListComponent),
        title: 'Notifiche - beetUs',
      },
      {
        path: 'search',
        loadComponent: () =>
          import('../search/search-results-component/search-results-component').then((m) => m.SearchResultsComponent),
        title: 'Cerca - beetUs',
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('../settings/settings-component/settings-component').then((m) => m.SettingsComponent),
        title: 'Impostazioni - beetUs',
      },
    ],
  },
];
