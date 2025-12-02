import { computed, Injectable, signal } from '@angular/core';
import { UserResponseDTO } from '../../models';

@Injectable({
  providedIn: 'root'
})
export class AuthStore {
  // Signal privato per l'utente corrente
  private readonly _currentUser = signal<UserResponseDTO | null>(null);

  // Signal readonly esposto pubblicamente
  readonly currentUser = this._currentUser.asReadonly();

  // Computed signal per verificare se l'utente è autenticato
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  // Computed signal per verificare se l'utente è admin
  readonly isAdmin = computed(() => this._currentUser()?.isAdmin ?? false);

  // Computed signal per verificare se l'utente è attivo
  readonly isActive = computed(() => this._currentUser()?.isActive ?? false);

  // Computed signal per ottenere l'username
  readonly username = computed(() => this._currentUser()?.username ?? null);

  // Computed signal per ottenere l'ID utente
  readonly userId = computed(() => this._currentUser()?.id ?? null);

  /**
   * Imposta l'utente corrente 
   */
  setUser(user: UserResponseDTO): void {
    this._currentUser.set(user);
  }

  /**
   * Aggiorna parzialmente i dati dell'utente corrente
   * Utile per aggiornamenti profilo senza re-login
   */
  updateUser(updates: Partial<UserResponseDTO>): void {
    const currentUser = this._currentUser();
    
    if (currentUser) {
      this._currentUser.set({
        ...currentUser,
        ...updates,
      });
    }
  }

  /**
   * Pulisce lo store (dopo logout)
   */
  clearUser(): void {
    this._currentUser.set(null);
  }

  /**
   * Verifica se l'utente corrente è il proprietario di una risorsa
   */
  isOwner(userId: number): boolean {
    const currentUserId = this._currentUser()?.id;
    return currentUserId !== null && currentUserId === userId;
  }
  /**
   * Imposta un utente mock per scopi di test e sviluppo
   * @param isAdmin Se true, l'utente avrà privilegi di amministratore
   */
  setMockUser(isAdmin: boolean): void {
    const mockUser: UserResponseDTO = {
      id: 1, // Corrisponde all'autore "Mario Rossi" in app.ts
      username: 'mario_rossi',
      email: 'mario.rossi@example.com',
      nomeCompleto: 'Mario Rossi',
      bio: 'Appassionato di tecnologia e sviluppo web. Benvenuti nel mio profilo!',
      profilePictureUrl: null, // O un URL valido se preferisci
      isAdmin: isAdmin,
      isActive: true,
      lastSeen: new Date().toISOString(),
      isOnline: true,
    };

    this._currentUser.set(mockUser);
  }
}
