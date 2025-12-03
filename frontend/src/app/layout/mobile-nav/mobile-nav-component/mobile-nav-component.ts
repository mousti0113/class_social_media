import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, Home, Search, PlusSquare, Bell, User } from 'lucide-angular';

import { AuthStore } from '../../../core/stores/auth-store';
import { NotificationStore } from '../../../core/stores/notification-store';

@Component({
  selector: 'app-mobile-nav',
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    LucideAngularModule,
  ],
  templateUrl: './mobile-nav-component.html',
  styleUrl: './mobile-nav-component.scss',
})
export class MobileNavComponent {
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);
  private readonly notificationStore = inject(NotificationStore);

  // Icone Lucide
  readonly HomeIcon = Home;
  readonly SearchIcon = Search;
  readonly PlusIcon = PlusSquare;
  readonly BellIcon = Bell;
  readonly UserIcon = User;

  // ========== COMPUTED ==========

  /** ID utente corrente */
  readonly userId = computed(() => this.authStore.userId());

  /** Conteggio notifiche non lette */
  readonly unreadCount = computed(() => this.notificationStore.unreadCount());

  /** Ha notifiche non lette? */
  readonly hasUnread = computed(() => this.notificationStore.hasUnread());

  /** URL profilo utente */
  readonly profileUrl = computed(() => {
    const id = this.userId();
    return id ? `/profile/${id}` : '/profile';
  });

  // ========== ACTIONS ==========

  /**
   * Naviga alla home
   */
  goToHome(): void {
    this.router.navigate(['/']);
  }

  /**
   * Naviga alla ricerca
   */
  goToSearch(): void {
    this.router.navigate(['/search']);
  }

  /**
   * Apri modal per creare post (emette evento o naviga)
   */
  createPost(): void {
    // Per ora naviga a una pagina dedicata, 
    // oppure può emettere un evento per aprire un modal
    this.router.navigate(['/post/create']);
  }

  /**
   * Naviga alle notifiche
   */
  goToNotifications(): void {
    this.router.navigate(['/notifications']);
  }

  /**
   * Naviga al profilo
   */
  goToProfile(): void {
    const id = this.userId();
    if (id) {
      this.router.navigate(['/profile', id]);
    }
  }
}
