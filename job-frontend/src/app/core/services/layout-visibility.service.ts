import { computed, Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LayoutVisibilityService {
  private readonly hiddenHeaderRoutes = new Set([
    '/login-callback',
    '/candidate-list',
    '/post-job',
  ]);

  private readonly hiddenHeaderPrefixes = ['/recruiter', '/admin'];
  private readonly hiddenFooterPrefixes = ['/recruiter', '/admin', '/infor'];
  private readonly headerSignal =  signal<boolean>(false);
  private readonly footerSignal = signal<boolean>(false);
  headerComputed = computed(() => this.headerSignal());
  footerComputed = computed(() => this.footerSignal());

  checkUrlIsHidden(url: string) {
    const path = this.normalizePath(url);
    const isHeaderHidden = this.hiddenHeaderRoutes.has(path) || this.hiddenHeaderPrefixes.some(prefix => path.startsWith(prefix));
    const isFooterHidden = this.hiddenFooterPrefixes.some(prefix => path.startsWith(prefix));
    this.headerSignal.set(!isHeaderHidden);
    this.footerSignal.set(!isFooterHidden);
  }

  private normalizePath(url: string): string {
    return (url || '/').split('?')[0].split('#')[0];
  }
}
