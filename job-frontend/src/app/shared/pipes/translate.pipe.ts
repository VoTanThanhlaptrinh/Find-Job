import { Pipe, PipeTransform } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';

@Pipe({
  name: 't',
  standalone: true,
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  constructor(private readonly i18nService: I18nService) {}

  transform(key: string | null | undefined): string {
    if (!key) {
      return '';
    }

    return this.i18nService.translate(key);
  }
}
