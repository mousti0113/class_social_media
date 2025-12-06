import { Component, inject, OnInit, OnDestroy, signal, computed, ElementRef, viewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, ArrowLeft, Send, Image, Trash } from 'lucide-angular';
import { Subscription, interval, Subject } from 'rxjs';
import { switchMap, startWith, takeUntil, finalize } from 'rxjs/operators';
import { MessageService } from '../../../../core/api/message-service';
import { UserService } from '../../../../core/api/user-service';
import { WebsocketService } from '../../../../core/services/websocket-service';
import { AuthStore } from '../../../../core/stores/auth-store';
import { OnlineUsersStore } from '../../../../core/stores/online-users-store';
import { MessageResponseDTO, UserSummaryDTO } from '../../../../models';
import { MessageBubbleComponent } from '../../../../shared/components/message-bubble/message-bubble-component/message-bubble-component';
import { AvatarComponent } from '../../../../shared/ui/avatar/avatar-component/avatar-component';
import { SkeletonComponent } from '../../../../shared/ui/skeleton/skeleton-component/skeleton-component';

/**
 * Chat view per una singola conversazione.
 * Su mobile: schermo intero con freccia per tornare alla lista.
 * Su desktop: colonna destra del layout messaggi.
 */
@Component({
  selector: 'app-chat-component',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    MessageBubbleComponent,
    AvatarComponent,
    SkeletonComponent,
  ],
  templateUrl: './chat-component.html',
  styleUrl: './chat-component.scss',
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  private readonly messagesContainer = viewChild<ElementRef<HTMLDivElement>>('messagesContainer');
  
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly userService = inject(UserService);
  private readonly websocketService = inject(WebsocketService);
  private readonly authStore = inject(AuthStore);
  private readonly onlineUsersStore = inject(OnlineUsersStore);
  
  private routeSub?: Subscription;
  private wsMessagesSub?: Subscription;
  private readonly pollingStop$ = new Subject<void>();
  private shouldScrollToBottom = false;
  private currentOtherUserId: number | null = null;
  private lastTypingSent = 0;

  // Polling intervals
  private readonly MESSAGE_POLLING_INTERVAL = 3000;
  private readonly TYPING_POLLING_INTERVAL = 2000;
  private readonly TYPING_THROTTLE = 2000;

  // Icone
  readonly ArrowLeftIcon = ArrowLeft;
  readonly SendIcon = Send;
  readonly ImageIcon = Image;
  readonly TrashIcon = Trash;

  // Stato
  readonly otherUser = signal<UserSummaryDTO | null>(null);
  readonly messages = signal<MessageResponseDTO[]>([]);
  readonly isLoading = signal(true);
  readonly isSending = signal(false);
  readonly deletingIds = signal<Set<number>>(new Set());
  readonly error = signal<string | null>(null);
  readonly messageText = signal('');
  readonly isOtherUserTyping = signal(false);

  // ID utente corrente
  readonly currentUserId = computed(() => this.authStore.userId());

  // Verifica se l'altro utente è online
  readonly isOtherUserOnline = computed(() => {
    const user = this.otherUser();
    if (!user) return false;
    return this.onlineUsersStore.isUserOnline(user.id);
  });

  // Stato online text
  readonly onlineStatusText = computed(() => {
    if (this.isOtherUserTyping()) {
      return 'Sta scrivendo...';
    }
    return this.isOtherUserOnline() ? 'Online' : 'Offline';
  });

  // Può inviare messaggio
  readonly canSend = computed(() => {
    return this.messageText().trim().length > 0 && !this.isSending();
  });

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe(params => {
      const userIdParam = params.get('userId');
      if (userIdParam) {
        const userId = Number.parseInt(userIdParam, 10);
        this.switchToConversation(userId);
      }
    });

    // Sottoscrizione ai messaggi WebSocket real-time
    this.wsMessagesSub = this.websocketService.messages$.subscribe({
      next: (newMessage: MessageResponseDTO) => {
        console.log('[Chat] Nuovo messaggio WebSocket ricevuto:', newMessage);
        
        // Verifica se il messaggio appartiene alla conversazione corrente
        const isRelevant = 
          (newMessage.mittente.id === this.currentOtherUserId && newMessage.destinatario.id === this.currentUserId()) ||
          (newMessage.destinatario.id === this.currentOtherUserId && newMessage.mittente.id === this.currentUserId());
        
        if (isRelevant) {
          // Aggiungi il messaggio se non esiste già (evita duplicati dal polling)
          const currentMessages = this.messages();
          const exists = currentMessages.some(m => m.id === newMessage.id);
          
          if (!exists) {
            this.messages.set([...currentMessages, newMessage]);
            this.shouldScrollToBottom = true;
            console.log('[Chat] Messaggio aggiunto alla conversazione');
          }
        }
      },
      error: (err) => {
        console.error('[Chat] Errore WebSocket messages:', err);
      }
    });
  }

  ngOnDestroy(): void {
    // Clear typing quando lascia la chat
    if (this.currentOtherUserId) {
      this.messageService.clearTyping(this.currentOtherUserId).subscribe();
    }
    
    this.stopPolling();
    this.pollingStop$.complete();
    this.routeSub?.unsubscribe();
    this.wsMessagesSub?.unsubscribe();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  /**
   * Cambia conversazione - ferma polling vecchio e inizia nuovo
   */
  private switchToConversation(userId: number): void {
    // Se è la stessa conversazione, non fare nulla
    if (this.currentOtherUserId === userId) {
      return;
    }

    // Ferma polling della conversazione precedente
    this.stopPolling();
    
    // Reset stato
    this.currentOtherUserId = userId;
    this.messages.set([]);
    this.otherUser.set(null);
    this.isLoading.set(true);
    this.error.set(null);
    this.isOtherUserTyping.set(false);
    this.messageText.set('');
    
    // Carica nuova conversazione
    this.loadUserInfo(userId);
    this.startPolling(userId);
  }

  /**
   * Ferma tutti i polling attivi
   */
  private stopPolling(): void {
    this.pollingStop$.next();
  }

  /**
   * Carica info utente
   */
  private loadUserInfo(userId: number): void {
    this.userService.getUserProfile(userId).subscribe({
      next: (user) => {
        // Verifica che sia ancora la stessa conversazione
        if (this.currentOtherUserId !== userId) return;
        
        this.otherUser.set({
          id: user.id,
          username: user.username,
          nomeCompleto: user.nomeCompleto,
          profilePictureUrl: user.profilePictureUrl,
          isOnline: user.isOnline,
        });
      },
      error: (err) => {
        if (this.currentOtherUserId !== userId) return;
        console.error('Errore caricamento utente:', err);
        this.error.set('Utente non trovato');
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Avvia polling per messaggi e typing
   */
  private startPolling(userId: number): void {
    // Polling messaggi
    interval(this.MESSAGE_POLLING_INTERVAL)
      .pipe(
        startWith(0),
        takeUntil(this.pollingStop$),
        switchMap(() => this.messageService.getConversation(userId))
      )
      .subscribe({
        next: (messages) => {
          // Verifica che sia ancora la stessa conversazione
          if (this.currentOtherUserId !== userId) return;
          
          const currentMessages = this.messages();
          const hadMessages = currentMessages.length > 0;
          const newMessagesCount = messages.length - currentMessages.length;
          
          this.messages.set(messages);
          this.isLoading.set(false);
          
          // Scrolla solo se ci sono nuovi messaggi o è il primo caricamento
          if (!hadMessages || newMessagesCount > 0) {
            this.shouldScrollToBottom = true;
          }
          
          // Marca come letti
          if (messages.length > 0) {
            this.messageService.markConversationAsRead(userId).subscribe();
          }
        },
        error: (err) => {
          if (this.currentOtherUserId !== userId) return;
          console.error('Errore caricamento messaggi:', err);
          if (this.isLoading()) {
            this.error.set('Impossibile caricare i messaggi');
            this.isLoading.set(false);
          }
        },
      });

    // Polling typing
    interval(this.TYPING_POLLING_INTERVAL)
      .pipe(
        startWith(0),
        takeUntil(this.pollingStop$),
        switchMap(() => this.messageService.isTyping(userId))
      )
      .subscribe({
        next: (response) => {
          if (this.currentOtherUserId !== userId) return;
          this.isOtherUserTyping.set(response.isTyping);
        },
        error: () => {
          // Ignora errori di typing
        },
      });
  }

  /**
   * Gestisce input del messaggio e invia typing indicator
   */
  onMessageInput(value: string): void {
    this.messageText.set(value);
    
    // Invia typing indicator (throttled)
    const now = Date.now();
    if (this.currentOtherUserId && now - this.lastTypingSent > this.TYPING_THROTTLE) {
      this.lastTypingSent = now;
      this.messageService.setTyping(this.currentOtherUserId).subscribe();
    }
  }

  sendMessage(): void {
    const content = this.messageText().trim();
    const user = this.otherUser();
    
    if (!content || !user || this.isSending()) return;

    this.isSending.set(true);
    const targetUserId = user.id;

    // Clear typing quando invia
    this.messageService.clearTyping(targetUserId).subscribe();

    this.messageService.sendMessage({
      destinatarioId: targetUserId,
      contenuto: content,
    }).subscribe({
      next: (newMessage) => {
        // Verifica che sia ancora la stessa conversazione
        if (this.currentOtherUserId === targetUserId) {
          this.messages.update(msgs => [...msgs, newMessage]);
          this.messageText.set('');
          this.shouldScrollToBottom = true;
        }
        this.isSending.set(false);
      },
      error: (err) => {
        console.error('Errore invio messaggio:', err);
        this.isSending.set(false);
      },
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  goBack(): void {
    this.router.navigate(['/messages']);
  }

  private scrollToBottom(): void {
    const container = this.messagesContainer();
    if (container) {
      const el = container.nativeElement;
      el.scrollTop = el.scrollHeight;
    }
  }

  trackByMessageId(index: number, message: MessageResponseDTO): number {
    return message.id;
  }

  /**
   * Elimina un messaggio.
   * - Se è un mio messaggio: soft delete (tutti vedono "Messaggio cancellato")
   * - Se è un messaggio altrui: nascondimento (solo io non lo vedo più)
   */
  deleteMessage(messageId: number): void {
    if (this.deletingIds().has(messageId)) return;

    this.deletingIds.update(set => new Set(set).add(messageId));

    this.messageService.deleteMessage(messageId)
      .pipe(finalize(() => {
        this.deletingIds.update(set => {
          const next = new Set(set);
          next.delete(messageId);
          return next;
        });
      }))
      .subscribe({
        next: () => {
          const currUserId = this.currentUserId();
          this.messages.update(msgs =>
            msgs.map(m => {
              if (m.id === messageId) {
                // Se sono il mittente -> soft delete (aggiorna flag)
                if (m.mittente.id === currUserId) {
                  return { ...m, isDeletedBySender: true };
                }
                // Se sono il destinatario -> il backend lo ha nascosto, lo rimuovo dalla lista
                return null;
              }
              return m;
            }).filter((m): m is MessageResponseDTO => m !== null)
          );
        },
        error: (err) => {
          console.error('Errore eliminazione messaggio:', err);
        },
      });
  }

  /**
   * Verifica se il messaggio è stato eliminato dal mittente
   */
  isMessageDeleted(message: MessageResponseDTO): boolean {
    return message.isDeletedBySender;
  }

  isDeleting(messageId: number): boolean {
    return this.deletingIds().has(messageId);
  }
}
