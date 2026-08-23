import {
  Component,
  HostListener,
  Inject,
  OnInit,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { NavigationEnd, Router, RouterModule } from "@angular/router";
import { I18nService } from '../../i18n/i18n.service';
import { AppLanguage } from '../../i18n/translations';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { MobileNavComponent } from '../mobile-nav/mobile-nav.component';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthModalComponent } from '../../../features/auth/components/auth-modal/auth-modal.component';
import { ActivatedRoute } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-header',
  imports: [
    CommonModule,
    MatButtonModule,
    MatMenuModule,
    MatDialogModule,
    RouterModule,
    TranslatePipe,
    MobileNavComponent,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  standalone: true,
})
export class HeaderComponent implements OnInit {
  isScrolled = false;
  activeNav: 'home' | 'jobs' | 'blog' | 'about' | 'contact' = 'home';

  constructor(
    private auth: AuthService,
    private i18nService: I18nService,
    private router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.route.queryParams.subscribe(params => {
      if (params['auth'] === 'login') {
        this.openAuthModal('login');
        this.router.navigate([], { queryParams: { auth: null }, queryParamsHandling: 'merge', replaceUrl: true });
      } else if (params['auth'] === 'register') {
        this.openAuthModal('register');
        this.router.navigate([], { queryParams: { auth: null }, queryParamsHandling: 'merge', replaceUrl: true });
      }
    });
  }

  ngOnInit(): void {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.updateActiveNavFromRoute(event.urlAfterRedirects || event.url);
      });

    this.route.fragment.subscribe(fragment => {
      if (fragment) {
        setTimeout(() => this.scrollToElement(fragment), 150);
      }
    });
  }

  private updateActiveNavFromRoute(url: string): void {
    if (url.includes('/category') || url.includes('/single') || url.includes('/jobs')) {
      this.activeNav = 'jobs';
    } else if (url.includes('/blogHome') || url.includes('/blogSingle') || url.includes('/blog')) {
      this.activeNav = 'blog';
    } else if (url.includes('#about')) {
      this.activeNav = 'about';
    } else if (url.includes('#contact')) {
      this.activeNav = 'contact';
    } else if (url === '/' || url.startsWith('/?')) {
      this.checkScrollspy();
    }
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.isScrolled = window.scrollY > 30;
      if (this.router.url === '/' || this.router.url.startsWith('/#') || this.router.url.startsWith('/?')) {
        this.checkScrollspy();
      }
    }
  }

  private checkScrollspy(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const contactEl = document.getElementById('contact');
    const aboutEl = document.getElementById('about');
    const scrollPos = window.scrollY + 200;

    if (contactEl && scrollPos >= contactEl.offsetTop) {
      this.activeNav = 'contact';
    } else if (aboutEl && scrollPos >= aboutEl.offsetTop) {
      this.activeNav = 'about';
    } else {
      this.activeNav = 'home';
    }
  }

  navigateTo(target: 'home' | 'jobs' | 'blog' | 'about' | 'contact'): void {
    if (target === 'home') {
      if (this.router.url === '/' || this.router.url.startsWith('/#')) {
        window.scrollTo({ top: 0, behavior: 'smooth' });
        this.activeNav = 'home';
      } else {
        this.router.navigate(['/']);
      }
    } else if (target === 'jobs') {
      this.router.navigate(['/category']);
    } else if (target === 'blog') {
      this.router.navigate(['/blogHome']);
    } else if (target === 'about' || target === 'contact') {
      const isHome = this.router.url === '/' || this.router.url.startsWith('/#') || this.router.url.startsWith('/?');
      if (isHome) {
        this.scrollToElement(target);
        this.activeNav = target;
      } else {
        this.router.navigate(['/'], { fragment: target });
      }
    }
  }

  private scrollToElement(id: string): void {
    if (isPlatformBrowser(this.platformId)) {
      const el = document.getElementById(id);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
  }

  openAuthModal(tab: 'login' | 'register'): void {
    this.dialog.open(AuthModalComponent, {
      width: 'auto',
      maxWidth: '95vw',
      panelClass: 'auth-modal-panel',
      data: { initialTab: tab }
    });
  }

  logout(): void {
    this.auth.logout('/login');
  }

  goInfor(): void {
    this.router.navigate(['/infor']);
  }

  goVerify(): void {
    this.router.navigate(['/verify']);
  }

  get isLoginPage(): boolean {
    return this.auth.isLoginClicked();
  }

  get isRegisterPage(): boolean {
    return this.auth.isRegisterClicked();
  }

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  get isAuthReady(): boolean {
    return this.auth.isAuthReady();
  }

  get currentLanguage(): AppLanguage {
    return this.i18nService.currentLanguage;
  }

  switchLanguage(language: AppLanguage): void {
    this.i18nService.setLanguage(language);
  }
}
