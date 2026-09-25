import { Component, effect, OnInit } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/layout/header/header.component';
import { FooterComponent } from './core/layout/footer/footer.component';
import { filter } from 'rxjs/operators';
import { LayoutVisibilityService } from './core/services/layout-visibility.service';
import { AuthService } from './core/services/auth.service';
import { I18nService } from './core/i18n/i18n.service';

import { BackToTopComponent } from './shared/components/back-to-top/back-to-top.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, FooterComponent, BackToTopComponent],
  standalone: true,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  title = 'job-list';
  showHeader = true;
  showFooter = true;
  isResolvingAuth = false;

  constructor(
    private router: Router,
    private layoutVisibilityService: LayoutVisibilityService,
    private authService: AuthService,
    private i18nService: I18nService,
  ) {
    this.showHeader = this.layoutVisibilityService.headerComputed();
    this.showFooter = this.layoutVisibilityService.footerComputed();

    effect(() => {
      this.showHeader = this.layoutVisibilityService.headerComputed();
      this.showFooter = this.layoutVisibilityService.footerComputed();
    });
  }

  ngOnInit(): void {
    this.i18nService.initialize();
    this.layoutVisibilityService.checkUrlIsHidden(this.router.url);
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        if ((event.url.startsWith('/admin') || event.url.startsWith('/recruiter') || event.url.startsWith('/blog-creation')) && !this.authService.isAuthReady()) {
          this.isResolvingAuth = true;
        }
      } else if (event instanceof NavigationEnd) {
        this.layoutVisibilityService.checkUrlIsHidden(event.urlAfterRedirects);
        this.isResolvingAuth = false;
      } else if (event instanceof NavigationCancel || event instanceof NavigationError) {
        this.isResolvingAuth = false;
      }
    });
  }
}
