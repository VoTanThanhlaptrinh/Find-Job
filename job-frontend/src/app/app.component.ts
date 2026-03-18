import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/layout/header/header.component';
import { FooterComponent } from './core/layout/footer/footer.component';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  title = 'job-list';
  showHeader = true;
  showFooter = true;

  private readonly hiddenHeaderRoutes = new Set([
    '/login',
    '/register',
    '/forgot-pass',
    '/verify',
    '/activate',
    '/login-callback',
  ]);

  private readonly hiddenLayoutPrefixes = ['/recruiter/'];

  constructor(private router: Router) {
    this.updateHeaderVisibility(this.router.url);

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navigationEvent = event as NavigationEnd;
        this.updateHeaderVisibility(navigationEvent.urlAfterRedirects);
      });
  }

  private updateHeaderVisibility(url: string): void {
    const path = this.normalizePath(url);
    const isHidden = this.isHiddenHeaderRoute(path);
    this.showHeader = !isHidden;
    this.showFooter = !isHidden;
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
