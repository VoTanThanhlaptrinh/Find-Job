import { Pipe, PipeTransform, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import DOMPurify from 'dompurify';

@Pipe({
  name: 'safeHtml',
  standalone: true,
})
export class SafeHtmlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): string {
    const raw = value ?? '';
    if (!raw) {
      return '';
    }
    if (typeof window !== 'undefined') {
      const purify = (DOMPurify as any).default || DOMPurify;
      if (typeof purify.sanitize === 'function') {
        return purify.sanitize(raw);
      }
    }
    return this.sanitizer.sanitize(SecurityContext.HTML, raw) ?? '';
  }
}