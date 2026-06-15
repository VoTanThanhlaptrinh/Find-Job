import { Component, effect, OnInit, inject, PLATFORM_ID } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/layout/header/header.component';
import { FooterComponent } from './core/layout/footer/footer.component';
import { filter } from 'rxjs/operators';
import { LayoutVisibilityService } from './core/services/layout-visibility.service';
import { AuthService } from './core/services/auth.service';
import { I18nService } from './core/i18n/i18n.service';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  title = 'job-list';
  showHeader = false;
  showFooter = false;
  private platformId = inject(PLATFORM_ID);

  constructor(
    private router: Router,
    private layoutVisibilityService: LayoutVisibilityService,
    private authService: AuthService,
    private i18nService: I18nService,
  ) {
    effect(() => {
      this.showHeader = this.layoutVisibilityService.headerComputed();
      this.showFooter = this.layoutVisibilityService.footerComputed();
    });
  }

  ngOnInit(): void {
    this.i18nService.initialize();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navigationEvent = event as NavigationEnd;
        this.layoutVisibilityService.checkUrlIsHidden(navigationEvent.urlAfterRedirects);
      });
    this.hideLoadingScreen();
  }

  private hideLoadingScreen(): void {
    if (isPlatformBrowser(this.platformId)) {
      const loadingEl = document.getElementById('app-loading-screen');
      if (loadingEl) {
        loadingEl.classList.add('app-loading--hidden');
        loadingEl.addEventListener('transitionend', () => {
          loadingEl.remove();
        });
      }
    }
  }
}
