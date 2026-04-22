import { Injectable } from '@angular/core';

export interface LayoutVisibility {
  showHeader: boolean;
  showFooter: boolean;
  path: string;
  hiddenHeaderReason: string | null;
  hiddenFooterReason: string | null;
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

  private readonly hiddenFooterRoutes = new Set<string>([]);
  private readonly hiddenHeaderPrefixes = ['/recruiter', '/admin'];
  private readonly hiddenFooterPrefixes = ['/recruiter', '/admin', '/infor'];

  getLayoutVisibility(url: string): LayoutVisibility {
    const path = this.normalizePath(url);
    const hiddenHeaderReason = this.getHeaderHiddenReason(path);
    const hiddenFooterReason = this.getFooterHiddenReason(path);

    return {
      showHeader: hiddenHeaderReason === null,
      showFooter: hiddenFooterReason === null,
      path,
      hiddenHeaderReason,
      hiddenFooterReason,
    };
  }

  private getHeaderHiddenReason(path: string): string | null {
    if (this.hiddenHeaderRoutes.has(path)) {
      return `matched hidden header route: ${path}`;
    }

    if (path.startsWith('/reset-pass/')) {
      return 'matched hidden header reset password route';
    }

    const matchedPrefix = this.hiddenHeaderPrefixes.find((prefix) =>
      path === prefix || path.startsWith(`${prefix}/`)
    );

    return matchedPrefix
      ? `matched hidden header prefix: ${matchedPrefix}`
      : null;
  }

  private getFooterHiddenReason(path: string): string | null {
    if (this.hiddenFooterRoutes.has(path)) {
      return `matched hidden footer route: ${path}`;
    }

    if (path.startsWith('/reset-pass/')) {
      return 'matched hidden footer reset password route';
    }

    const matchedPrefix = this.hiddenFooterPrefixes.find((prefix) =>
      path === prefix || path.startsWith(`${prefix}/`)
    );

    return matchedPrefix
      ? `matched hidden footer prefix: ${matchedPrefix}`
      : null;
  }

  private normalizePath(url: string): string {
    return (url || '/').split('?')[0].split('#')[0];
  }
}
