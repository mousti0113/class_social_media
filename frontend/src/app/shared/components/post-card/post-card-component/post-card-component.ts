import { Component, input, output, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  LucideAngularModule,
  Ellipsis,
  Pencil,
  Trash2,
  EyeOff,
  Flag,
} from 'lucide-angular';

import { PostResponseDTO } from '../../../../models';
import { AvatarComponent } from '../../../ui/avatar/avatar-component/avatar-component';
import { DropdownComponent } from '../../../ui/dropdown/dropdown-component/dropdown-component';
import { PostActionsComponent } from '../../post-actions/post-actions-component/post-actions-component';
import { TimeAgoPipe } from '../../../pipes/time-ago.pipe';
import { HighlightMentionPipe } from '../../../pipes/highlight-mention.pipe';
import { DialogService } from '../../../../core/services/dialog-service';
import { ToastService } from '../../../../core/services/toast-service';
import { AuthStore } from '../../../../core/stores/auth-store';
import { PostService } from '../../../../core/api/post-service';

@Component({
  selector: 'app-post-card-component',
  imports: [
    CommonModule,
    LucideAngularModule,
    AvatarComponent,
    DropdownComponent,
    PostActionsComponent,
    TimeAgoPipe,
    HighlightMentionPipe,
  ],
  templateUrl: './post-card-component.html',
  styleUrl: './post-card-component.scss',
})
export class PostCardComponent {
  private readonly router = inject(Router);
  private readonly postService = inject(PostService);
  private readonly dialogService = inject(DialogService);
  private readonly toastService = inject(ToastService);
  private readonly authStore = inject(AuthStore);

  // Icone Lucide
  readonly EllipsisIcon = Ellipsis;
  readonly PencilIcon = Pencil;
  readonly Trash2Icon = Trash2;
  readonly EyeOffIcon = EyeOff;
  readonly FlagIcon = Flag;

  /**
   * Dati del post da visualizzare
   */
  readonly post = input.required<PostResponseDTO>();

  /**
   * Mostra le azioni (like, commenti)
   * @default true
   */
  readonly showActions = input<boolean>(true);

  /**
   * Rende il post cliccabile per navigare al dettaglio
   * @default true
   */
  readonly clickable = input<boolean>(true);


  /**
   * Emesso quando il post viene eliminato
   */
  readonly deleted = output<number>();

  /**
   * Emesso quando il post viene modificato
   */
  readonly edited = output<PostResponseDTO>();

  /**
   * Emesso quando il post viene nascosto
   */
  readonly hidden = output<number>();


  /**
   * Verifica se l'utente corrente è l'autore del post
   */
  readonly isOwner = computed(() => {
    return this.authStore.isOwner(this.post().autore.id);
  });

  /**
   * Verifica se l'utente corrente è admin
   */
  readonly isAdmin = computed(() => this.authStore.isAdmin());


  /**
   * Naviga al profilo dell'autore
   */
  goToAuthor(event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/profile', this.post().autore.id]);
  }

  /**
   * Naviga al dettaglio del post
   */
  goToPost(): void {
    if (this.clickable()) {
      this.router.navigate(['/post', this.post().id]);
    }
  }

  /**
   * Apre i commenti (naviga al dettaglio)
   */
  openComments(event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/post', this.post().id], { fragment: 'comments' });
  }

  /**
   * Modifica post
   * TODO: Implementare form di modifica
   */
  editPost(event: Event): void {
    event.stopPropagation();
    // Per ora mostra un messaggio, poi implementeremo il form di modifica
    this.toastService.info('Funzionalità modifica in arrivo');
    // Quando sarà pronto: this.router.navigate(['/post', this.post().id, 'edit']);
  }

  /**
   * Elimina post con conferma
   */
  async deletePost(event: Event): Promise<void> {
    event.stopPropagation();

    const confirmed = await this.dialogService.confirmDangerous({
      title: 'Elimina post',
      message: 'Sei sicuro di voler eliminare questo post? Questa azione non può essere annullata.',
      confirmText: 'Elimina',
      cancelText: 'Annulla',
    });

    if (!confirmed) return;

    this.postService.deletePost(this.post().id).subscribe({
      next: () => {
        this.toastService.success('Post eliminato');
        this.deleted.emit(this.post().id);
      },
      error: () => {
        this.toastService.error("Errore nell'eliminazione del post");
      },
    });
  }

  /**
   * Nascondi post
   */
  hidePost(event: Event): void {
    event.stopPropagation();

    this.postService.hidePost(this.post().id).subscribe({
      next: () => {
        this.toastService.success('Post nascosto');
        this.hidden.emit(this.post().id);
      },
      error: () => {
        this.toastService.error('Errore nel nascondere il post');
      },
    });
  }

}
