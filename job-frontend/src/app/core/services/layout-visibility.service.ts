import { Injectable } from '@angular/core';

export interface LayoutVisibility {
  showHeader: boolean;
  showFooter: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class LayoutVisibilityService {
  private readonly hiddenHeaderRoutes = new Set([
    '/login',
    '/register',
    '/forgot-pass',
    '/verify',
    '/activate',
    '/login-callback',
  ]);

  private readonly hiddenLayoutPrefixes = ['/recruiter/'];

  getLayoutVisibility(url: string): LayoutVisibility {
    const path = this.normalizePath(url);
    const isHidden = this.isHiddenHeaderRoute(path);

    return {
      showHeader: !isHidden,
      showFooter: !isHidden,
    };
  }

  private isHiddenHeaderRoute(path: string): boolean {
    if (this.hiddenHeaderRoutes.has(path)) {
      return true;
    }

    if (path.startsWith('/reset-pass/')) {
      return true;
    }

    return this.hiddenLayoutPrefixes.some((prefix) => path.startsWith(prefix));
  }

  private normalizePath(url: string): string {
    return (url || '/').split('?')[0].split('#')[0];
  }
}
