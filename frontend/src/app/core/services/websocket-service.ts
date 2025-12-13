import { Injectable, signal, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { TokenService } from '../auth/services/token-service';

@Injectable({
  providedIn: 'root',
})
export class WebsocketService {
  private readonly tokenService = inject(TokenService);

  private client: Client | null = null;
  private reconnectAttempts = 0;
  private readonly maxReconnectAttempts = 5;
  private readonly reconnectDelay = 3000; // 3 secondi

  // Signal per lo stato della connessione
  public readonly connected = signal<boolean>(false);

  // Subjects per i vari tipi di messaggi in arrivo
  private readonly notificationsSubject = new Subject<any>();
  private readonly messagesSubject = new Subject<any>();
  private readonly typingSubject = new Subject<TypingIndicator>();
  private readonly errorsSubject = new Subject<WebSocketError>();
  private readonly announcementsSubject = new Subject<any>();
  private readonly newPostsSubject = new Subject<any>();
  private readonly postUpdatedSubject = new Subject<any>();
  private readonly postDeletedSubject = new Subject<any>();
  private readonly postLikedSubject = new Subject<PostLikeUpdate>();
  private readonly commentUpdatesSubject = new Subject<CommentUpdate>();
  private readonly commentsCountSubject = new Subject<CommentsCountUpdate>();
  private readonly userPresenceSubject = new Subject<UserPresenceEvent>();

  // Observables pubblici per sottoscrizioni
  public notifications$ = this.notificationsSubject.asObservable();
  public messages$ = this.messagesSubject.asObservable();
  public typing$ = this.typingSubject.asObservable();
  public errors$ = this.errorsSubject.asObservable();
  public announcements$ = this.announcementsSubject.asObservable();
  public newPosts$ = this.newPostsSubject.asObservable();
  public postUpdated$ = this.postUpdatedSubject.asObservable();
  public postDeleted$ = this.postDeletedSubject.asObservable();
  public postLiked$ = this.postLikedSubject.asObservable();
  public commentUpdates$ = this.commentUpdatesSubject.asObservable();
  public commentsCount$ = this.commentsCountSubject.asObservable();
  public userPresence$ = this.userPresenceSubject.asObservable();

  // Mappa per tenere traccia delle sottoscrizioni ai commenti per post
  private commentSubscriptions = new Map<number, any>();

  /**
   * Connette al WebSocket server
   *
   * Endpoint: ws://localhost:8080/ws
   * Protocollo: STOMP over WebSocket con fallback SockJS
   *
   * Richiede token JWT per autenticazione
   */
  connect(): void {
    if (this.client?.connected) {
      return;
    }

    const token = this.tokenService.getAccessToken();
    if (!token) {
      console.error('[WebSocket] Token non disponibile, impossibile connettersi');
      return;
    }

    this.client = new Client({
      // Usa SockJS come trasporto (fallback per browser senza WebSocket nativo)
      webSocketFactory: () => new SockJS(environment.wsUrl),

      // Header di connessione (include token JWT)
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },

      // Debug (disabilita in produzione)
      debug: (str) => {
        // Debug logging disabled in production
      },

      // Heartbeat per mantenere la connessione attiva
      heartbeatIncoming: 10000, // 10 secondi
      heartbeatOutgoing: 10000,

      // Riconnessione automatica
      reconnectDelay: this.reconnectDelay,

      // Callback successo connessione
      onConnect: () => {
        this.onConnected();
      },

      // Callback errore connessione
      onStompError: (frame) => {
        this.onError(frame);
      },

      // Callback disconnessione
      onDisconnect: () => {
        this.onDisconnected();
      },

      // Callback cambio stato WebSocket
      onWebSocketClose: () => {
        this.connected.set(false);
      },
    });

    // Attiva la connessione
    this.client.activate();
  }

  /**
   * Disconnette dal WebSocket server
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
      this.connected.set(false);
    }
  }

  /**
   * Callback chiamato quando la connessione è stabilita
   */
  private onConnected(): void {
    this.connected.set(true);
    this.reconnectAttempts = 0;

    // Sottoscrizioni ai canali
    this.subscribeToChannels();
  }

  /**
   * Callback chiamato quando si verifica un errore
   */
  private onError(frame: any): void {
    this.connected.set(false);

    // Tentativo di riconnessione
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
    }
  }

  /**
   * Callback chiamato quando la connessione viene chiusa
   */
  private onDisconnected(): void {
    this.connected.set(false);
  }

  /**
   * Sottoscrive ai vari canali WebSocket
   */
  private subscribeToChannels(): void {
    if (!this.client) return;

    // Sottoscrizione alle notifiche personali
    this.client.subscribe('/user/queue/notifications', (message: IMessage) => {
      const notification = JSON.parse(message.body);
      this.notificationsSubject.next(notification);
    });

    // Sottoscrizione ai messaggi diretti
    this.client.subscribe('/user/queue/messages', (message: IMessage) => {
      const msg = JSON.parse(message.body);
      this.messagesSubject.next(msg);
    });

    // Sottoscrizione agli indicatori di digitazione
    this.client.subscribe('/user/queue/typing', (message: IMessage) => {
      const typing = JSON.parse(message.body);
      this.typingSubject.next(typing);
    });

    // Sottoscrizione agli errori
    this.client.subscribe('/user/queue/errors', (message: IMessage) => {
      const error = JSON.parse(message.body);
      console.error('[WebSocket] Errore ricevuto:', error);
      this.errorsSubject.next(error);
    });

    // Sottoscrizione agli annunci broadcast
    this.client.subscribe('/topic/announcements', (message: IMessage) => {
      const announcement = JSON.parse(message.body);
      this.announcementsSubject.next(announcement);
    });

    // Sottoscrizione ai nuovi post (broadcast a tutti gli utenti)
    this.client.subscribe('/topic/posts', (message: IMessage) => {
      const post = JSON.parse(message.body);
      this.newPostsSubject.next(post);
    });

    // Sottoscrizione ai post aggiornati
    this.client.subscribe('/topic/posts/updated', (message: IMessage) => {
      const post = JSON.parse(message.body);
      this.postUpdatedSubject.next(post);
    });

    // Sottoscrizione ai post cancellati
    this.client.subscribe('/topic/posts/deleted', (message: IMessage) => {
      const data = JSON.parse(message.body);
      this.postDeletedSubject.next(data);
    });

    // Sottoscrizione agli aggiornamenti like
    this.client.subscribe('/topic/posts/liked', (message: IMessage) => {
      const likeUpdate = JSON.parse(message.body);
      this.postLikedSubject.next(likeUpdate);
    });

    // Sottoscrizione agli aggiornamenti conteggio commenti (globale)
    this.client.subscribe('/topic/posts/comments-count', (message: IMessage) => {
      const countUpdate = JSON.parse(message.body);
      this.commentsCountSubject.next(countUpdate);
    });

    // Sottoscrizione agli eventi di presenza utenti (online/offline)
    this.client.subscribe('/topic/user-presence', (message: IMessage) => {
      const presenceEvent = JSON.parse(message.body);
      this.userPresenceSubject.next(presenceEvent);
    });
  }

  /**
   * Invia un messaggio di test al server
   *
   * Destination: /app/test
   *
   * @param content contenuto del messaggio
   * @param type tipo opzionale del messaggio
   */
  sendTestMessage(content: string, type?: string): void {
    if (!this.client?.connected) {
      console.error('[WebSocket] Non connesso, impossibile inviare messaggio');
      return;
    }

    this.client.publish({
      destination: '/app/test',
      body: JSON.stringify({ content, type }),
    });
  }

  /**
   * Invia un indicatore di digitazione
   *
   * Destination: /app/typing
   *
   * Notifica al destinatario che l'utente sta scrivendo un messaggio
   *
   * @param recipientUsername username del destinatario
   * @param isTyping true se sta digitando, false se ha smesso
   */
  sendTypingIndicator(recipientUsername: string, isTyping: boolean): void {
    if (!this.client?.connected) {
      console.warn('[WebSocket] Non connesso, impossibile inviare typing indicator');
      return;
    }

    this.client.publish({
      destination: '/app/typing',
      body: JSON.stringify({
        recipientUsername,
        isTyping,
      }),
    });
  }

  /**
   * Sottoscrive agli aggiornamenti dei commenti per un post specifico
   *
   * @param postId ID del post
   */
  subscribeToPostComments(postId: number): void {
    if (!this.client?.connected) {
      console.warn('[WebSocket] Non connesso, impossibile sottoscrivere ai commenti');
      return;
    }

    // Evita sottoscrizioni duplicate
    if (this.commentSubscriptions.has(postId)) {
      return;
    }

    const subscription = this.client.subscribe(
      `/topic/posts/${postId}/comments`,
      (message: IMessage) => {
        const update = JSON.parse(message.body);
        this.commentUpdatesSubject.next(update);
      }
    );

    this.commentSubscriptions.set(postId, subscription);
  }

  /**
   * Cancella la sottoscrizione ai commenti di un post
   *
   * @param postId ID del post
   */
  unsubscribeFromPostComments(postId: number): void {
    const subscription = this.commentSubscriptions.get(postId);
    if (subscription) {
      subscription.unsubscribe();
      this.commentSubscriptions.delete(postId);
    }
  }

  /**
   * Verifica se il WebSocket è connesso
   */
  isConnected(): boolean {
    return this.client?.connected ?? false;
  }
}

/**
 * Interfaccia per l'indicatore di digitazione
 */
export interface TypingIndicator {
  /** Username dell'utente che sta digitando */
  senderUsername: string;

  /** True se sta digitando, false se ha smesso */
  isTyping: boolean;
}

/**
 * Interfaccia per gli errori WebSocket
 */
export interface WebSocketError {
  /** Flag errore */
  error: boolean;

  /** Messaggio di errore */
  message: string;

  /** Timestamp dell'errore */
  timestamp: number;
}

/**
 * Interfaccia per aggiornamenti like sui post
 */
export interface PostLikeUpdate {
  /** ID del post */
  postId: number;

  /** Nuovo conteggio dei like */
  likesCount: number;

  /** True se è stato aggiunto un like, false se rimosso */
  liked: boolean;

  /** Tipo dell'aggiornamento */
  type: 'like_update';
}

/**
 * Interfaccia per aggiornamenti commenti
 */
export interface CommentUpdate {
  /** ID del post */
  postId: number;

  /** Tipo dell'aggiornamento */
  type: 'comment_created' | 'comment_updated' | 'comment_deleted';

  /** Commento (presente per created e updated) */
  comment?: any;

  /** ID del commento (presente per deleted) */
  commentId?: number;

  /** ID del commento padre (presente per risposte) */
  parentCommentId?: number | null;
}

/**
 * Interfaccia per aggiornamenti conteggio commenti
 */
export interface CommentsCountUpdate {
  /** ID del post */
  postId: number;

  /** Nuovo conteggio dei commenti */
  commentsCount: number;

  /** Tipo dell'aggiornamento */
  type: 'comments_count_update';
}

/**
 * Interfaccia per eventi di presenza utenti (online/offline)
 */
export interface UserPresenceEvent {
  /** Tipo dell'evento */
  type: 'user_online' | 'user_offline';

  /** ID dell'utente */
  userId: number;

  /** Username dell'utente */
  username: string;

  /** Nome completo dell'utente */
  nomeCompleto: string;

  /** URL immagine profilo */
  profilePictureUrl: string | null;

  /** Stato online */
  isOnline: boolean;

  /** Timestamp dell'evento */
  timestamp: number;
}
