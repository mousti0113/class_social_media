import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  ArrowLeft,
  User,
  Lock,
  Palette,
  Power,
  Camera,
  Save,
  Eye,
  EyeOff,
  Check,
  TriangleAlert,
  Sun,
  Moon,
  Monitor,
} from 'lucide-angular';
import { Subject, takeUntil, finalize } from 'rxjs';

import { UserService } from '../../../core/api/user-service';
import { AuthStore } from '../../../core/stores/auth-store';
import { ThemeStore, Theme } from '../../../core/stores/theme-store';
import { AuthService } from '../../../core/auth/services/auth-service';
import { ToastService } from '../../../core/services/toast-service';
import { DialogService } from '../../../core/services/dialog-service';
import { CloudinaryStorageService } from '../../../core/services/cloudinary-storage-service';
import { AvatarComponent } from '../../../shared/ui/avatar/avatar-component/avatar-component';
import { ButtonComponent } from '../../../shared/ui/button/button-component/button-component';
import { AggiornaProfiloRequestDTO, CambiaPasswordRequestDTO, DisattivaAccountRequestDTO } from '../../../models';

type SettingsSection = 'profile' | 'password' | 'theme' | 'account';
type ThemeMode = Theme | 'system';

@Component({
  selector: 'app-settings-component',
  imports: [
    CommonModule,
    FormsModule,
    LucideAngularModule,
    AvatarComponent,
    ButtonComponent,
  ],
  templateUrl: './settings-component.html',
  styleUrl: './settings-component.scss',
})
export class SettingsComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly userService = inject(UserService);
  private readonly authStore = inject(AuthStore);
  private readonly themeStore = inject(ThemeStore);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly dialogService = inject(DialogService);
  private readonly cloudinaryService = inject(CloudinaryStorageService);
  private readonly destroy$ = new Subject<void>();

  // Icone
  readonly ArrowLeftIcon = ArrowLeft;
  readonly UserIcon = User;
  readonly LockIcon = Lock;
  readonly PaletteIcon = Palette;
  readonly PowerIcon = Power;
  readonly CameraIcon = Camera;
  readonly SaveIcon = Save;
  readonly EyeIcon = Eye;
  readonly EyeOffIcon = EyeOff;
  readonly CheckIcon = Check;
  readonly AlertTriangleIcon = TriangleAlert;
  readonly SunIcon = Sun;
  readonly MoonIcon = Moon;
  readonly MonitorIcon = Monitor;

  // State
  readonly activeSection = signal<SettingsSection>('profile');
  readonly isSaving = signal(false);

  // Profile form
  readonly nomeCompleto = signal('');
  readonly bio = signal('');
  readonly profilePictureUrl = signal<string | null>(null);
  readonly isUploadingImage = signal(false);

  // Password form
  readonly vecchiaPassword = signal('');
  readonly nuovaPassword = signal('');
  readonly confermaPassword = signal('');
  readonly showOldPassword = signal(false);
  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  // Theme
  readonly currentTheme = signal<ThemeMode>('light');

  // Deactivate form
  readonly deactivatePassword = signal('');
  readonly showDeactivatePassword = signal(false);

  // Computed
  readonly currentUser = computed(() => this.authStore.currentUser());
  
  readonly passwordsMatch = computed(() => {
    const nuova = this.nuovaPassword();
    const conferma = this.confermaPassword();
    return nuova === conferma;
  });

  readonly isPasswordValid = computed(() => {
    const nuova = this.nuovaPassword();
    return nuova.length >= 8;
  });

  readonly canSavePassword = computed(() => {
    return this.vecchiaPassword().length > 0 &&
           this.isPasswordValid() &&
           this.passwordsMatch();
  });

  readonly canDeactivate = computed(() => {
    return this.deactivatePassword().length > 0;
  });

  readonly sections: { id: SettingsSection; label: string; icon: typeof User }[] = [
    { id: 'profile', label: 'Profilo', icon: User },
    { id: 'password', label: 'Password', icon: Lock },
    { id: 'theme', label: 'Tema', icon: Palette },
    { id: 'account', label: 'Account', icon: Power },
  ];

  ngOnInit(): void {
    this.loadUserData();
    this.loadThemePreference();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Carica i dati utente correnti
   */
  private loadUserData(): void {
    const user = this.currentUser();
    if (user) {
      this.nomeCompleto.set(user.nomeCompleto);
      this.bio.set(user.bio || '');
      this.profilePictureUrl.set(user.profilePictureUrl);
    }
  }

  /**
   * Carica la preferenza tema dal ThemeStore
   */
  private loadThemePreference(): void {
    // Controlla se c'è una preferenza salvata
    if (this.themeStore.hasSavedPreference()) {
      // Usa la preferenza salvata
      this.currentTheme.set(this.themeStore.currentTheme());
    } else {
      // Nessuna preferenza salvata, usa 'system'
      this.currentTheme.set('system');
      // Ma applica comunque la preferenza di sistema
      const systemTheme = this.themeStore.getSystemThemePreference();
      this.themeStore.setTheme(systemTheme);
    }
  }

  /**
   * Cambia sezione attiva
   */
  setSection(section: SettingsSection): void {
    this.activeSection.set(section);
  }

  /**
   * Salva modifiche profilo
   */
  saveProfile(): void {
    this.isSaving.set(true);

    const request: AggiornaProfiloRequestDTO = {
      nomeCompleto: this.nomeCompleto(),
      bio: this.bio() || undefined,
      profilePictureUrl: this.profilePictureUrl() || undefined,
    };

    this.userService.updateProfile(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: (updatedUser) => {
          this.authStore.updateUser(updatedUser);
          this.toastService.success('Profilo aggiornato con successo');
        },
        error: () => {
          this.toastService.error('Errore durante il salvataggio');
        }
      });
  }

  /**
   * Gestisce upload immagine profilo
   */
  onImageUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    
    if (!file) return;

    this.isUploadingImage.set(true);

    // Upload su Cloudinary
    this.cloudinaryService.uploadImage(file, 'profile', (progress) => {
      // Opzionale: mostra barra di progresso
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isUploadingImage.set(false))
      )
      .subscribe({
        next: (response) => {
          this.profilePictureUrl.set(response.secureUrl);
          this.toastService.success('Immagine caricata. Salva per applicare le modifiche.');
        },
        error: (error) => {
          const message = error.message || 'Errore durante il caricamento dell\'immagine';
          this.toastService.error(message);
        }
      });

    // Reset input per permettere ri-caricamento stesso file
    input.value = '';
  }

  /**
   * Rimuove immagine profilo
   */
  async removeProfileImage(): Promise<void> {
    const currentUrl = this.profilePictureUrl();
    
    if (!currentUrl) return;

    // Conferma eliminazione
    const confirmed = await this.dialogService.confirm({
      title: 'Rimuovi foto profilo',
      message: 'Sei sicuro di voler rimuovere la foto profilo?',
      confirmText: 'Rimuovi',
      cancelText: 'Annulla',
    });

    if (!confirmed) return;

    this.isUploadingImage.set(true);

    // Se l'immagine è su Cloudinary, eliminala
    if (currentUrl.includes('cloudinary.com')) {
      this.cloudinaryService.deleteImage(currentUrl)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.isUploadingImage.set(false))
        )
        .subscribe({
          next: () => {
            this.profilePictureUrl.set(null);
            this.toastService.success('Foto profilo rimossa. Salva per applicare le modifiche.');
          },
          error: (error) => {
            console.error('Errore eliminazione immagine:', error);
            // Anche se l'eliminazione da Cloudinary fallisce, permetti di rimuovere l'URL
            this.profilePictureUrl.set(null);
            this.toastService.warning('Immagine rimossa dal profilo. Salva per applicare le modifiche.');
          }
        });
    } else {
      // URL non Cloudinary (es. base64 vecchio), rimuovi solo localmente
      this.profilePictureUrl.set(null);
      this.isUploadingImage.set(false);
      this.toastService.success('Foto profilo rimossa. Salva per applicare le modifiche.');
    }
  }

  /**
   * Cambia password
   */
  changePassword(): void {
    if (!this.canSavePassword()) return;

    this.isSaving.set(true);

    const request: CambiaPasswordRequestDTO = {
      vecchiaPassword: this.vecchiaPassword(),
      nuovaPassword: this.nuovaPassword(),
    };

    this.userService.changePassword(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('Password modificata con successo');
          this.resetPasswordForm();
        },
        error: (error) => {
          const message = error.error?.message || 'Errore durante il cambio password';
          this.toastService.error(message);
        }
      });
  }

  /**
   * Reset form password
   */
  private resetPasswordForm(): void {
    this.vecchiaPassword.set('');
    this.nuovaPassword.set('');
    this.confermaPassword.set('');
  }

  /**
   * Cambia tema
   */
  setTheme(theme: ThemeMode): void {
    this.currentTheme.set(theme);

    if (theme === 'system') {
      // Usa la preferenza di sistema
      this.themeStore.useSystemPreference();
    } else {
      // Imposta il tema specifico
      this.themeStore.setTheme(theme);
    }

    this.toastService.success('Tema applicato');
  }

  /**
   * Disattiva account
   */
  async deactivateAccount(): Promise<void> {
    if (!this.canDeactivate()) return;

    const confirmed = await this.dialogService.confirmDangerous({
      title: 'Disattiva account',
      message: 'Sei sicuro di voler disattivare il tuo account? Non potrai più accedere alla piattaforma fino a quando non contatterai l\'assistenza.',
      confirmText: 'Disattiva',
      cancelText: 'Annulla',
    });

    if (!confirmed) return;

    this.isSaving.set(true);

    const request: DisattivaAccountRequestDTO = {
      password: this.deactivatePassword(),
    };

    this.userService.deactivateAccount(request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isSaving.set(false))
      )
      .subscribe({
        next: () => {
          this.toastService.success('Account disattivato');
          this.authService.logout();
          this.router.navigate(['/auth/login']);
        },
        error: (error) => {
          const message = error.error?.message || 'Errore durante la disattivazione';
          this.toastService.error(message);
        }
      });
  }

  /**
   * Toggle visibilità password
   */
  togglePasswordVisibility(field: 'old' | 'new' | 'confirm' | 'deactivate'): void {
    switch (field) {
      case 'old':
        this.showOldPassword.update(v => !v);
        break;
      case 'new':
        this.showNewPassword.update(v => !v);
        break;
      case 'confirm':
        this.showConfirmPassword.update(v => !v);
        break;
      case 'deactivate':
        this.showDeactivatePassword.update(v => !v);
        break;
    }
  }

  /**
   * Torna indietro
   */
  goBack(): void {
    this.location.back();
  }
}

