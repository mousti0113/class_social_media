import { Routes } from '@angular/router';
import { authGuard } from '../../core/auth/guards/auth-guard';
import { MainLayoutComponent } from '../../layout/main-layout/main-layout-component/main-layout-component';

export const homeRoutes: Routes = [
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
    ],
  },
];
