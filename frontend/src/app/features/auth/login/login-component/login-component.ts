import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputComponent } from '../../../../shared/ui/input/input-component/input-component';
import { ButtonComponent } from '../../../../shared/ui/button/button-component/button-component';
import { AuthService } from '../../../../core/auth/services/auth-service';
import { ToastService } from '../../../../core/services/toast-service';
import { passwordValidator, usernameValidator } from '../../../../core/utils/validators';

@Component({
  selector: 'app-login-component',
  imports: [ CommonModule,
    ReactiveFormsModule,
    RouterLink,
    InputComponent,
    ButtonComponent,],
  templateUrl: './login-component.html',
  styleUrl: './login-component.scss',
})
export class LoginComponent {
private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  // Stato UI
  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string>('');

  // Form di login
  readonly loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, usernameValidator()]],
    password: ['', [Validators.required, passwordValidator()]],
  });

  /**
   * Ottiene il messaggio di errore per un campo
   */
  getFieldError(fieldName: string): string {
    const control = this.loginForm.get(fieldName);

    if (!control?.errors || !control.touched) {
      return '';
    }

    // Errore required
    if (control.errors['required']) {
      return `${this.getFieldLabel(fieldName)} è obbligatorio`;
    }

    // Errore custom dal validatore
    if (control.errors['username']) {
      return control.errors['username'].message;
    }

    if (control.errors['password']) {
      return control.errors['password'].message;
    }

    return '';
  }

  /**
   * Restituisce la label del campo
   */
  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      username: 'Username',
      password: 'Password',
    };
    return labels[fieldName] || fieldName;
  }

  /**
   * Gestisce il submit del form
   */
  onSubmit(): void {
    // Marca tutti i campi come touched per mostrare errori
    this.loginForm.markAllAsTouched();

    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: () => {
        this.toastService.success('Accesso effettuato con successo!');
        this.router.navigate(['/']);
      },
      error: (error) => {
        this.isLoading.set(false);

        // Gestione errori specifici
        if (error.status === 401) {
          this.errorMessage.set('Credenziali non valide. Riprova.');
        } else if (error.status === 0) {
          this.errorMessage.set('Impossibile connettersi al server. Verifica la connessione.');
        } else {
          this.errorMessage.set(error.error?.message || 'Errore durante il login. Riprova.');
        }
      },
    });
  }
}
