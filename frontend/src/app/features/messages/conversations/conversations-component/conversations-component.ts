import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, ArrowLeft, MessageSquare, Search, X } from 'lucide-angular';
import { Subscription, interval, Subject } from 'rxjs';
import { switchMap, startWith, debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { MessageService } from '../../../../core/api/message-service';
import { OnlineUsersStore } from '../../../../core/stores/online-users-store';
import { TypingStore } from '../../../../core/stores/typing-store';
import { ConversationResponseDTO, MessageResponseDTO } from '../../../../models';
import { ConversationItemComponent } from '../../../../shared/components/conversation-item/conversation-item-component/conversation-item-component';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton-component/skeleton-component';
import { AvatarComponent } from '../../../../shared/ui/avatar/avatar-component/avatar-component';
import { TimeAgoPipe } from '../../../../shared/pipes/time-ago.pipe';

/**
 * Lista delle conversazioni dell'utente.
 * Su mobile: schermo intero con freccia per tornare alla home.
 * Su desktop: colonna sinistra del layout messaggi.
 * 
 * La ricerca funziona in stile WhatsApp:
 * - Mostra risultati da TUTTI i messaggi, non solo dalle conversazioni
 * - Cliccando su un risultato si va alla chat e si evidenzia il messaggio
 */
@Component({
  selector: 'app-conversations-component',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    ConversationItemComponent,
    SkeletonComponent,
    AvatarComponent,
    TimeAgoPipe,
  ],
  templateUrl: './conversations-component.html',
  styleUrl: './conversations-component.scss',
})
export class ConversationsComponent implements OnInit, OnDestroy {
  private readonly messageService = inject(MessageService);
  private readonly onlineUsersStore = inject(OnlineUsersStore);
  private readonly typingStore = inject(TypingStore);
  private readonly router = inject(Router);
  private readonly subscriptions: Subscription[] = [];
  private readonly searchSubject = new Subject<string>();

  // Icone
  readonly ArrowLeftIcon = ArrowLeft;
  readonly MessageSquareIcon = MessageSquare;
  readonly SearchIcon = Search;
  readonly XIcon = X;

  // Stato
  readonly conversations = signal<ConversationResponseDTO[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedUserId = signal<number | null>(null);
  readonly searchQuery = signal('');
  
  // Stato ricerca messaggi
  readonly searchResults = signal<MessageResponseDTO[]>([]);
  readonly isSearching = signal(false);
  readonly isSearchMode = computed(() => this.searchQuery().trim().length > 0);

  // Polling interval (5 secondi)
  private readonly POLLING_INTERVAL = 5000;

  // Computed - filtra per ricerca (solo conversazioni, non messaggi)
  readonly filteredConversations = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const convs = this.conversations();
    
    if (!query) return convs;
    
    // In modalità ricerca, filtra solo per nome utente (i messaggi sono separati)
    return convs.filter(conv => 
      conv.altroUtente.username.toLowerCase().includes(query) ||
      conv.altroUtente.nomeCompleto?.toLowerCase().includes(query)
    );
  });

  readonly hasConversations = computed(() => this.filteredConversations().length > 0);
  readonly hasSearchResults = computed(() => this.searchResults().length > 0);
  readonly isEmpty = computed(() => !this.isLoading() && !this.isSearchMode() && this.conversations().length === 0);
  readonly noSearchResults = computed(() => 
    !this.isLoading() && 
    !this.isSearching() &&
    this.isSearchMode() && 
    !this.hasConversations() && 
    !this.hasSearchResults()
  );

  ngOnInit(): void {
    this.startPolling();
    this.setupSearchDebounce();
    // Carica utenti online
    this.onlineUsersStore.loadOnlineUsers();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.searchSubject.complete();
    this.typingStore.stopPolling();
  }

  /**
   * Setup del debounce per la ricerca messaggi
   */
  private setupSearchDebounce(): void {
    const searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      filter(query => query.trim().length >= 2)
    ).subscribe(query => {
      this.performSearch(query);
    });
    this.subscriptions.push(searchSub);
  }

  /**
   * Esegue la ricerca messaggi
   */
  private performSearch(query: string): void {
    this.isSearching.set(true);
    this.messageService.searchMessages(query).subscribe({
      next: (results) => {
        this.searchResults.set(results);
        this.isSearching.set(false);
      },
      error: (err) => {
        console.error('Errore ricerca messaggi:', err);
        this.searchResults.set([]);
        this.isSearching.set(false);
      },
    });
  }

  /**
   * Avvia polling per aggiornamenti real-time
   */
  private startPolling(): void {
    const pollingSub = interval(this.POLLING_INTERVAL)
      .pipe(
        startWith(0),
        switchMap(() => this.messageService.getConversations(0, 50))
      )
      .subscribe({
        next: (response) => {
          this.conversations.set(response.content);
          this.isLoading.set(false);
          
          // Aggiorna la lista degli utenti da monitorare per il typing
          const userIds = response.content.map(conv => conv.altroUtente.id);
          this.typingStore.setMonitoredUsers(userIds);
        },
        error: (err) => {
          console.error('Errore caricamento conversazioni:', err);
          if (this.isLoading()) {
            this.error.set('Impossibile caricare le conversazioni');
            this.isLoading.set(false);
          }
        },
      });
    this.subscriptions.push(pollingSub);
  }

  /**
   * Verifica se un utente è online usando lo store
   */
  isUserOnline(userId: number): boolean {
    return this.onlineUsersStore.isUserOnline(userId);
  }

  /**
   * Verifica se un utente sta scrivendo
   */
  isUserTyping(userId: number): boolean {
    return this.typingStore.isUserTyping(userId);
  }

  onConversationSelect(userId: number): void {
    this.selectedUserId.set(userId);
    this.router.navigate(['/messages', userId]);
  }

  /**
   * Naviga a un messaggio specifico dalla ricerca
   */
  onSearchResultClick(message: MessageResponseDTO): void {
    // Determina l'altro utente (mittente o destinatario)
    const otherId = message.mittente.id;
    this.selectedUserId.set(otherId);
    // Naviga alla chat con l'ID del messaggio come query param
    this.router.navigate(['/messages', otherId], {
      queryParams: { messageId: message.id }
    });
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  refresh(): void {
    this.isLoading.set(true);
    this.messageService.getConversations(0, 50).subscribe({
      next: (response) => {
        this.conversations.set(response.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Errore caricamento conversazioni:', err);
        this.error.set('Impossibile caricare le conversazioni');
        this.isLoading.set(false);
      },
    });
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value);
    
    if (value.trim().length >= 2) {
      this.searchSubject.next(value);
    } else {
      this.searchResults.set([]);
    }
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.searchResults.set([]);
  }

  /**
   * Evidenzia il termine di ricerca nel testo
   */
  highlightText(text: string, searchTerm: string): string {
    if (!searchTerm || !text) return text;
    const regex = new RegExp(`(${this.escapeRegex(searchTerm)})`, 'gi');
    return text.replace(regex, '<mark class="bg-yellow-500/30 text-white rounded px-0.5">$1</mark>');
  }

  private escapeRegex(string: string): string {
    return string.replaceAll(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}
