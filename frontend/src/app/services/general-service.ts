import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class GeneralService {
  isDarkMode = signal(false); // Segnale per tracciare lo stato

}
