import { Pipe, PipeTransform } from '@angular/core';
import { formatTimeAgo } from '../../core/utils/date.utils';

/**
 * Pipe per formattare date in formato relativo (es: "2 ore fa")
 *
 */
@Pipe({
  name: 'timeAgo',
  standalone: true,
  pure: false, // Impure per aggiornarsi quando cambia il tempo
})
export class TimeAgoPipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }

    // Converte Date object in stringa ISO se necessario
    const isoDate = value instanceof Date ? value.toISOString() : value;

    return formatTimeAgo(isoDate);
  }
}
