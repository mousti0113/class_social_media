import { Component, input, output, signal, inject, computed, viewChild, ElementRef, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Send, X } from 'lucide-angular';
import { CommentService } from '../../../../core/api/comment-service';
import { ToastService } from '../../../../core/services/toast-service';
import { AuthStore } from '../../../../core/stores/auth-store';
import { AutoResize } from '../../../directives/auto-resize';
import { AvatarComponent } from '../../../ui/avatar/avatar-component/avatar-component';
import { CommentResponseDTO, CreaCommentoRequestDTO } from '../../../../models';
@Component({
  selector: 'app-comment-form-component',
  imports: [CommonModule,
    LucideAngularModule,
    AvatarComponent,
    AutoResize,],
  templateUrl: './comment-form-component.html',
  styleUrl: './comment-form-component.scss',
})
export class CommentFormComponent {
private readonly commentService = inject(CommentService);
  private readonly toastService = inject(ToastService);
  private readonly authStore = inject(AuthStore);

  // Riferimento alla textarea per focus
  readonly textareaRef = viewChild<ElementRef<HTMLTextAreaElement>>('textareaEl');

  constructor() {
    // Quando replyTo cambia, focus e scroll sulla textarea
    effect(() => {
      const reply = this.replyTo();
      if (reply) {
        // setTimeout per aspettare il rendering
        setTimeout(() => {
          const textarea = this.textareaRef()?.nativeElement;
          if (textarea) {
            textarea.scrollIntoView({ behavior: 'smooth', block: 'center' });
            textarea.focus();
          }
        });
      }
    });
  }

  // Icone
  readonly SendIcon = Send;
  readonly XIcon = X;

  // ========== INPUTS ==========

  /**
   * ID del post a cui aggiungere il commento
   */
  readonly postId = input.required<number>();

  /**
   * Commento a cui si sta rispondendo (opzionale)
   * Se presente, il form è in modalità "risposta"
   */
  readonly replyTo = input<CommentResponseDTO | null>(null);

  /**
   * Placeholder personalizzato
   * @default 'Scrivi un commento...'
   */
  readonly placeholder = input<string>('Scrivi un commento...');

  // ========== OUTPUTS ==========

  /**
   * Emesso quando un commento viene creato con successo
   */
  readonly created = output<CommentResponseDTO>();

  /**
   * Emesso quando l'utente annulla la risposta
   */
  readonly cancelReply = output<void>();

  // ========== STATE ==========

  readonly content = signal<string>('');
  readonly isSubmitting = signal<boolean>(false);

  // ========== COMPUTED ==========

  /**
   * Utente corrente per avatar
   */
  readonly currentUser = computed(() => this.authStore.currentUser());

  /**
   * Verifica se è in modalità risposta
   */
  readonly isReplyMode = computed(() => this.replyTo() !== null);

  /**
   * Placeholder dinamico
   */
  readonly dynamicPlaceholder = computed(() => {
    const reply = this.replyTo();
    if (reply) {
      return `Rispondi a @${reply.autore.username}...`;
    }
    return this.placeholder();
  });

  /**
   * Disabilita submit se vuoto o in caricamento
   */
  readonly isSubmitDisabled = computed(() => {
    return !this.content().trim() || this.isSubmitting();
  });

  // ========== METHODS ==========

  /**
   * Gestisce input textarea
   */
  onInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.content.set(target.value);
  }

  /**
   * Invia il commento
   */
  submit(): void {
    const trimmedContent = this.content().trim();
    if (!trimmedContent || this.isSubmitting()) return;

    this.isSubmitting.set(true);

    const request: CreaCommentoRequestDTO = {
      contenuto: trimmedContent,
    };

    // Se è una risposta, aggiungi parentCommentId
    const reply = this.replyTo();
    if (reply) {
      request.parentCommentId = reply.id;
    }

    this.commentService.createComment(this.postId(), request).subscribe({
      next: (comment) => {
        this.content.set('');
        this.isSubmitting.set(false);
        this.created.emit(comment);
        
        if (reply) {
          this.toastService.success('Risposta pubblicata');
        } else {
          this.toastService.success('Commento pubblicato');
        }
      },
      error: () => {
        this.isSubmitting.set(false);
        this.toastService.error('Errore nella pubblicazione');
      },
    });
  }

  /**
   * Annulla risposta
   */
  cancel(): void {
    this.content.set('');
    this.cancelReply.emit();
  }

  /**
   * Gestisce invio con Ctrl+Enter
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault();
      this.submit();
    }
  }
}
