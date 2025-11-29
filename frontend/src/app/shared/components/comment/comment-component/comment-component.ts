import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { LucideAngularModule, Ellipsis, Pencil, Trash2, EyeOff, Flag, Reply, ChevronDown, ChevronUp } from 'lucide-angular';
import { AvatarComponent } from '../../../ui/avatar/avatar-component/avatar-component';
import { DropdownComponent } from '../../../ui/dropdown/dropdown-component/dropdown-component';
import { TimeAgoPipe } from '../../../pipes/time-ago.pipe';
import { HighlightMentionPipe } from '../../../pipes/highlight-mention.pipe';
import { CommentService } from '../../../../core/api/comment-service';
import { DialogService } from '../../../../core/services/dialog-service';
import { ToastService } from '../../../../core/services/toast-service';
import { AuthStore } from '../../../../core/stores/auth-store';
import { CommentResponseDTO } from '../../../../models';

@Component({
  selector: 'app-comment',
  imports: [CommonModule,
    LucideAngularModule,
    AvatarComponent,
    DropdownComponent,
    TimeAgoPipe,
    HighlightMentionPipe,],
  templateUrl: './comment-component.html',
  styleUrl: './comment-component.scss',
})
export class CommentComponent {
 private readonly commentService = inject(CommentService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);
  private readonly authStore = inject(AuthStore);

  // Icone Lucide
  readonly MoreHorizontalIcon = Ellipsis;
  readonly PencilIcon = Pencil;
  readonly Trash2Icon = Trash2;
  readonly EyeOffIcon = EyeOff;
  readonly FlagIcon = Flag;
  readonly ReplyIcon = Reply;
  readonly ChevronDownIcon = ChevronDown;
  readonly ChevronUpIcon = ChevronUp;

  // ========== INPUTS ==========

  /**
   * Dati del commento
   */
  readonly comment = input.required<CommentResponseDTO>();

  /**
   * ID del post (serve per le risposte)
   */
  readonly postId = input.required<number>();

  /**
   * Livello di nesting (0 = commento principale, 1 = risposta)
   * @default 0
   */
  readonly level = input<number>(0);

  // ========== OUTPUTS ==========

  /**
   * Emesso quando l'utente vuole rispondere
   */
  readonly reply = output<CommentResponseDTO>();

  /**
   * Emesso quando il commento viene eliminato
   */
  readonly deleted = output<number>();

  /**
   * Emesso quando il commento viene modificato
   */
  readonly updated = output<CommentResponseDTO>();

  // ========== STATE ==========

  readonly showReplies = signal<boolean>(true);
  readonly isEditing = signal<boolean>(false);
  readonly editContent = signal<string>('');

  // ========== COMPUTED ==========

  /**
   * Verifica se l'utente corrente è l'autore
   */
  readonly isOwner = computed(() => {
    const currentUserId = this.authStore.userId();
    return currentUserId !== null && currentUserId === this.comment().autore.id;
  });

  /**
   * Verifica se l'utente è admin
   */
  readonly isAdmin = computed(() => this.authStore.isAdmin());

  /**
   * Verifica se è una risposta (level > 0)
   */
  readonly isReply = computed(() => this.level() > 0);

  /**
   * Verifica se ha risposte
   */
  readonly hasReplies = computed(() => {
    const risposte = this.comment().risposte;
    return risposte && risposte.length > 0;
  });

  /**
   * Numero di risposte
   */
  readonly repliesCount = computed(() => this.comment().risposte?.length ?? 0);

  /**
   * Può rispondere (max 2 livelli)
   */
  readonly canReply = computed(() => this.level() < 1);

  // ========== METHODS ==========

  /**
   * Toggle visualizzazione risposte
   */
  toggleReplies(): void {
    this.showReplies.update(show => !show);
  }

  /**
   * Inizia risposta
   */
  startReply(): void {
    this.reply.emit(this.comment());
  }

  /**
   * Inizia modifica
   */
  startEdit(): void {
    this.editContent.set(this.comment().contenuto);
    this.isEditing.set(true);
  }

  /**
   * Annulla modifica
   */
  cancelEdit(): void {
    this.isEditing.set(false);
    this.editContent.set('');
  }

  /**
   * Salva modifica
   */
  saveEdit(): void {
    const newContent = this.editContent().trim();
    if (!newContent) return;

    this.commentService.updateComment(this.comment().id, newContent).subscribe({
      next: (updatedComment) => {
        this.isEditing.set(false);
        this.editContent.set('');
        this.updated.emit(updatedComment);
        this.toastService.success('Commento modificato');
      },
      error: () => {
        this.toastService.error('Errore nella modifica');
      },
    });
  }

  /**
   * Elimina commento
   */
  async deleteComment(): Promise<void> {
    const confirmed = await this.dialogService.confirmDangerous({
      title: 'Elimina commento',
      message: 'Sei sicuro di voler eliminare questo commento?',
      confirmText: 'Elimina',
      cancelText: 'Annulla',
    });

    if (!confirmed) return;

    this.commentService.deleteComment(this.comment().id).subscribe({
      next: () => {
        this.deleted.emit(this.comment().id);
        this.toastService.success('Commento eliminato');
      },
      error: () => {
        this.toastService.error('Errore nell\'eliminazione');
      },
    });
  }

  /**
   * Nascondi commento
   */
  hideComment(): void {
    this.commentService.hideComment(this.comment().id).subscribe({
      next: () => {
        this.deleted.emit(this.comment().id);
        this.toastService.success('Commento nascosto');
      },
      error: () => {
        this.toastService.error('Errore nel nascondere il commento');
      },
    });
  }



  /**
   * Gestisce input per edit
   */
  onEditInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.editContent.set(target.value);
  }

  /**
   * Propaga evento deleted dalle risposte
   */
  onReplyDeleted(commentId: number): void {
    this.deleted.emit(commentId);
  }

  /**
   * Propaga evento updated dalle risposte
   */
  onReplyUpdated(comment: CommentResponseDTO): void {
    this.updated.emit(comment);
  }

  /**
   * Propaga evento reply dalle risposte
   */
  onNestedReply(comment: CommentResponseDTO): void {
    this.reply.emit(comment);
  }
}
