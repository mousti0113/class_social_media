import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DialogComponent } from './shared/ui/dialog/dialog-component/dialog-component';
import { ToastComponent } from './shared/ui/toast/toast-component/toast-component';
import { MobileNavComponent } from './layout/mobile-nav/mobile-nav-component/mobile-nav-component';
import { ThemeStore } from './core/stores/theme-store';
import { AuthService } from './core/auth/services/auth-service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DialogComponent, ToastComponent, MobileNavComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit {
  // Inizializza il ThemeStore all'avvio per applicare il tema salvato in localStorage
  // Questo garantisce che il tema sia applicato anche sulle pagine pubbliche (login, register, ecc.)
  private readonly themeStore = inject(ThemeStore);

  // Inizializza l'AuthService per ripristinare la sessione da localStorage
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    // Ripristina la sessione utente dai token salvati in localStorage
    this.authService.initAuth();
  }

  // Global services like AuthStore, ToastService are injected in components or provided in root
}
