import { isPlatformBrowser } from '@angular/common';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UtilitiesService {
  private readonly apiBaseUrlStorageKey = 'job-list.apiBaseUrl';
  private readonly apiBaseUrl: string;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {
    this.apiBaseUrl = this.resolveApiBaseUrl();
  }

  getURLDev() {
    return this.apiBaseUrl;
  }

  getURLProduct() {
    return this.apiBaseUrl;
  }

  getApiBaseUrl() {
    return this.apiBaseUrl;
  }

  setApiBaseUrlForLocal(baseUrl: string) {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    localStorage.setItem(this.apiBaseUrlStorageKey, this.normalizeApiBaseUrl(baseUrl));
  }

  clearApiBaseUrlForLocal() {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    localStorage.removeItem(this.apiBaseUrlStorageKey);
  }

  private resolveApiBaseUrl(): string {
    const envApiUrl = this.normalizeApiBaseUrl(environment.apiBaseUrl);

    if (!isPlatformBrowser(this.platformId) || !environment.allowLocalApiOverride) {
      return envApiUrl;
    }

    const localApiUrl = localStorage.getItem(this.apiBaseUrlStorageKey);
    return localApiUrl ? this.normalizeApiBaseUrl(localApiUrl) : envApiUrl;
  }

  private normalizeApiBaseUrl(url: string): string {
    return (url || '').trim().replace(/\/+$/, '');
  }
}
