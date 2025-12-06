import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Pipe per evidenziare le menzioni @username nel testo
 *
 * Utilizzo:
 * <p [innerHTML]="post.content | highlightMention"></p>
 *
 * Output: Il testo con @username evidenziati come link cliccabili
 */
@Pipe({
  name: 'highlightMention',
  standalone: true,
})
export class HighlightMentionPipe implements PipeTransform {
  constructor(private readonly sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }

    // Regex per trovare @username (lettere, numeri, underscore)
    const mentionRegex = /@(\w+)/g;

    // Sostituisce le menzioni con span stilizzati
    const highlighted = value.replace(
      mentionRegex,
      '<a href="/search?q=$1&tab=users" class="mention-link text-primary-500 hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium hover:underline">@$1</a>'
    );

    // Sanitizza l'HTML per sicurezza
    return this.sanitizer.bypassSecurityTrustHtml(highlighted);
  }
}
