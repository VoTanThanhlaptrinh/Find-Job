import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { AppLanguage, TRANSLATIONS, TranslationTree } from './translations';

const LANGUAGE_STORAGE_KEY = 'job-list-language';

@Injectable({
  providedIn: 'root',
})
export class I18nService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly document = inject(DOCUMENT);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly languageSignal = signal<AppLanguage>('en');
  readonly language = this.languageSignal.asReadonly();
  readonly supportedLanguages: AppLanguage[] = ['vi', 'en'];

  initialize(): void {
    const initialLanguage = this.resolveInitialLanguage();
    this.applyLanguage(initialLanguage, false);
  }

  setLanguage(language: AppLanguage): void {
    this.applyLanguage(language, true);
  }

  get currentLanguage(): AppLanguage {
    return this.languageSignal();
  }

  translate(key: string): string {
    const language = this.languageSignal();
    const byCurrentLanguage = this.resolveTranslation(language, key);
    if (byCurrentLanguage !== null) {
      return byCurrentLanguage;
    }

    const fallback = this.resolveTranslation('en', key);
    return fallback ?? key;
  }

  private applyLanguage(language: AppLanguage, persist: boolean): void {
    this.languageSignal.set(language);

    if (this.isBrowser) {
      this.document.documentElement.lang = language;

      if (persist) {
        try {
          localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
        } catch {
          // Ignore localStorage errors silently (private mode, blocked storage, ...)
        }
      }
    }
  }

  private resolveInitialLanguage(): AppLanguage {
    if (!this.isBrowser) {
      return 'en';
    }

    const savedLanguage = this.getSavedLanguage();
    if (savedLanguage) {
      return savedLanguage;
    }

    return this.isVietnamRegion() ? 'vi' : 'en';
  }

  private getSavedLanguage(): AppLanguage | null {
    try {
      const saved = localStorage.getItem(LANGUAGE_STORAGE_KEY);
      if (saved === 'vi' || saved === 'en') {
        return saved;
      }
    } catch {
      // Ignore localStorage errors silently
    }

    return null;
  }

  private isVietnamRegion(): boolean {
    if (!this.isBrowser) {
      return false;
    }

    const navigatorLocales = [
      navigator.language,
      ...(navigator.languages ?? []),
      Intl.DateTimeFormat().resolvedOptions().locale,
    ]
      .filter(Boolean)
      .map((locale) => locale.toLowerCase());

    const hasVietnamLocale = navigatorLocales.some(
      (locale) =>
        locale === 'vi' ||
        locale.startsWith('vi-') ||
        locale.endsWith('-vn') ||
        locale.includes('_vn')
    );

    if (hasVietnamLocale) {
      return true;
    }

    const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone?.toLowerCase() ?? '';
    return timezone.includes('ho_chi_minh') || timezone.includes('saigon');
  }

  private resolveTranslation(language: AppLanguage, key: string): string | null {
    const segments = key.split('.').filter(Boolean);
    let current: string | TranslationTree = TRANSLATIONS[language];

    for (const segment of segments) {
      if (typeof current !== 'object' || current === null || !(segment in current)) {
        return null;
      }
      current = (current as TranslationTree)[segment];
    }

    return typeof current === 'string' ? current : null;
  }
}
