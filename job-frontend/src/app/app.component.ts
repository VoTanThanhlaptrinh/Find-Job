import { Component, effect, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
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
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navigationEvent = event as NavigationEnd;
        this.layoutVisibilityService.checkUrlIsHidden(navigationEvent.urlAfterRedirects);
      });
  }
}
