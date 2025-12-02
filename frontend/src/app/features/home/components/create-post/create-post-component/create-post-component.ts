import { Component, inject, signal, output, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Image, X } from 'lucide-angular';
import { AvatarComponent } from '../../../../../shared/ui/avatar/avatar-component/avatar-component';
import { ButtonComponent } from '../../../../../shared/ui/button/button-component/button-component';
import { ImageUploadComponent } from '../../../../../shared/components/image-upload/image-upload-component/image-upload-component';
import { PostService } from '../../../../../core/api/post-service';
import { AuthStore } from '../../../../../core/stores/auth-store';
import { ToastService } from '../../../../../core/services/toast-service';
import { CreaPostRequestDTO, PostResponseDTO } from '../../../../../models';

@Component({
  selector: 'app-create-post-component',
  imports: [ CommonModule,
    FormsModule,
    LucideAngularModule,
    AvatarComponent,
    ButtonComponent,
    ImageUploadComponent,],
  templateUrl: './create-post-component.html',
  styleUrl: './create-post-component.scss',
})
export class CreatePostComponent {
private readonly postService = inject(PostService);
  private readonly authStore = inject(AuthStore);
  private readonly toastService = inject(ToastService);

  // Icone Lucide
  readonly ImageIcon = Image;
  readonly XIcon = X;

  // Stato UI
  readonly content = signal<string>('');
  readonly imageUrl = signal<string | null>(null);
  readonly showImageUpload = signal<boolean>(false);
  readonly isSubmitting = signal<boolean>(false);

  // Lunghezza massima contenuto
  readonly MAX_CONTENT_LENGTH = 5000;

  /**
   * Emesso quando un nuovo post viene creato con successo
   */
  readonly postCreated = output<PostResponseDTO>();

  /**
   * Dati utente corrente
   */
  readonly currentUser = this.authStore.currentUser;

  /**
   * Placeholder dinamico con nome utente
   */
  readonly placeholder = computed(() => {
    const user = this.currentUser();
    const firstName = user?.nomeCompleto.split(' ')[0] || 'tu';
    return `A cosa stai pensando, ${firstName}?`;
  });

  /**
   * Verifica se il form è valido per il submit
   * Deve avere almeno contenuto o immagine
   */
  readonly canSubmit = computed(() => {
    const hasContent = this.content().trim().length > 0;
    const hasImage = !!this.imageUrl();
    const notSubmitting = !this.isSubmitting();
    const withinLimit = this.content().length <= this.MAX_CONTENT_LENGTH;

    return (hasContent || hasImage) && notSubmitting && withinLimit;
  });

  /**
   * Contatore caratteri
   */
  readonly charCount = computed(() => this.content().length);

  /**
   * Verifica se mostrare il contatore (quando si avvicina al limite)
   */
  readonly showCharCount = computed(() => {
    return this.charCount() > this.MAX_CONTENT_LENGTH - 500;
  });

  /**
   * Classi per il contatore caratteri
   */
  readonly charCountClasses = computed(() => {
    const remaining = this.MAX_CONTENT_LENGTH - this.charCount();

    if (remaining < 0) {
      return 'text-error-500 font-medium';
    }
    if (remaining < 100) {
      return 'text-warning-500';
    }
    return 'text-gray-400';
  });

  /**
   * Toggle visibilità upload immagine
   */
  toggleImageUpload(): void {
    this.showImageUpload.update((show) => !show);

    // Se nasconde l'upload, rimuove anche l'immagine
    if (!this.showImageUpload()) {
      this.imageUrl.set(null);
    }
  }

  /**
   * Gestisce il completamento dell'upload immagine
   */
  onImageUploaded(url: string): void {
    this.imageUrl.set(url);
  }

  /**
   * Gestisce la rimozione dell'immagine
   */
  onImageRemoved(): void {
    this.imageUrl.set(null);
  }

  /**
   * Gestisce errori di upload
   */
  onImageError(error: string): void {
    this.toastService.error(error);
  }

  /**
   * Rimuove l'immagine caricata
   */
  removeImage(): void {
    this.imageUrl.set(null);
    this.showImageUpload.set(false);
  }

  /**
   * Submit del post
   */
  submitPost(): void {
    if (!this.canSubmit()) return;

    this.isSubmitting.set(true);

    const request: CreaPostRequestDTO = {};

    // Aggiungi contenuto se presente
    const trimmedContent = this.content().trim();
    if (trimmedContent) {
      request.contenuto = trimmedContent;
    }

    // Aggiungi immagine se presente
    if (this.imageUrl()) {
      request.imageUrl = this.imageUrl()!;
    }

    this.postService.createPost(request).subscribe({
      next: (post) => {
        this.isSubmitting.set(false);
        this.resetForm();
        this.toastService.success('Post pubblicato!');
        this.postCreated.emit(post);
      },
      error: (error) => {
        this.isSubmitting.set(false);

        if (error.status === 400) {
          this.toastService.error('Il post deve contenere testo o un\'immagine');
        } else {
          this.toastService.error('Errore nella pubblicazione. Riprova.');
        }
      },
    });
  }

  /**
   * Resetta il form dopo il submit
   */
  private resetForm(): void {
    this.content.set('');
    this.imageUrl.set(null);
    this.showImageUpload.set(false);
  }
}
