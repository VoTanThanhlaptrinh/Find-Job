import { Injectable } from '@angular/core';

export interface LayoutVisibility {
  showHeader: boolean;
  showFooter: boolean;
  path: string;
  hiddenReason: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class LayoutVisibilityService {
  private readonly hiddenHeaderRoutes = new Set([
    '/login-callback',
    '/candidate-list',
    '/post-job',
  ]);

  private readonly hiddenLayoutPrefixes = ['/recruiter', '/admin'];

  getLayoutVisibility(url: string): LayoutVisibility {
    const path = this.normalizePath(url);
    const hiddenReason = this.getHiddenReason(path);
    const isHidden = hiddenReason !== null;

    return {
      showHeader: !isHidden,
      showFooter: !isHidden,
      path,
      hiddenReason,
    };
  }

  private getHiddenReason(path: string): string | null {
    if (this.hiddenHeaderRoutes.has(path)) {
      return `matched hidden route: ${path}`;
    }

    if (path.startsWith('/reset-pass/')) {
      return 'matched reset password route';
    }

    const matchedPrefix = this.hiddenLayoutPrefixes.find((prefix) =>
      path === prefix || path.startsWith(`${prefix}/`)
    );

    return matchedPrefix ? `matched hidden prefix: ${matchedPrefix}` : null;
  }

  private normalizePath(url: string): string {
    return (url || '/').split('?')[0].split('#')[0];
  }
}
