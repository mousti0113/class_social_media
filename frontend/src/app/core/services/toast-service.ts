import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: string;
  type: ToastType;
  message: string;
  duration?: number;
  dismissible?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  // State privato - lista toast attivi
  private readonly _toasts = signal<Toast[]>([]);
  
  // Selector pubblico readonly
  readonly toasts = this._toasts.asReadonly();

  // Configurazione
  private readonly DEFAULT_DURATION = 3000;
  private readonly MAX_TOASTS = 5;

  /**
   * Mostra toast di successo
   */
  success(message: string, duration?: number): string {
    return this.show({
      type: 'success',
      message,
      duration
    });
  }

  /**
   * Mostra toast di errore
   */
  error(message: string, duration?: number): string {
    return this.show({
      type: 'error',
      message,
      duration
    });
  }

  /**
   * Mostra toast di warning
   */
  warning(message: string, duration?: number): string {
    return this.show({
      type: 'warning',
      message,
      duration
    });
  }

  /**
   * Mostra toast informativo
   */
  info(message: string, duration?: number): string {
    return this.show({
      type: 'info',
      message,
      duration
    });
  }

  /**
   * Mostra toast generico
   * Restituisce l'ID del toast creato
   */
  show(options: Omit<Toast, 'id'>): string {
    const toast: Toast = {
      id: this.generateId(),
      type: options.type,
      message: options.message,
      duration: options.duration ?? this.DEFAULT_DURATION,
      dismissible: options.dismissible ?? true
    };

    const currentToasts = this._toasts();
    
    // Limita numero massimo toast simultanei
    if (currentToasts.length >= this.MAX_TOASTS) {
      this._toasts.set([...currentToasts.slice(1), toast]);
    } else {
      this._toasts.set([...currentToasts, toast]);
    }

    // Auto-dismiss dopo durata specificata
    if(toast.duration !== undefined && toast.duration !== null ){
    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(toast.id), toast.duration);
    }

    }

    return toast.id;
  }

  /**
   * Rimuove un toast specifico
   */
  dismiss(id: string): void {
    const currentToasts = this._toasts();
    this._toasts.set(currentToasts.filter(toast => toast.id !== id));
  }

  /**
   * Rimuove tutti i toast
   */
  dismissAll(): void {
    this._toasts.set([]);
  }

  /**
   * Genera ID univoco per il toast
   */
  private generateId(): string {
    return `toast_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
  }
}