import { Component, effect, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';
import { RecruiterAuthService } from '../../services/recruiter-auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { AuthService } from '../../../../core/services/auth.service';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { AppLanguage } from '../../../../core/i18n/translations';

export interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-recruiter-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './recruiter-layout.component.html',
  styleUrl: './recruiter-layout.component.css',
})
export class RecruiterLayoutComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly recruiterAuth = inject(RecruiterAuthService);
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly i18nService = inject(I18nService);

  readonly hasLoadedRoles = signal<boolean>(false);
  readonly hirerRoles = signal<string[]>([]);
  readonly isMobileSidebarOpen = signal<boolean>(false);
  readonly pageTitle = signal<string>('Overview');
  
  readonly navItems: NavItem[] = [
    { label: 'Overview', icon: 'dashboard', route: '/recruiter/dashboard' },
    { label: 'Jobs', icon: 'work', route: '/recruiter/jobs' },
    { label: 'Company Address', icon: 'location_on', route: '/recruiter/company-address' }
  ];
  readonly username = signal<string>('Recruiter');
  readonly avatarLetter = signal<string>('R');

  protected readonly openGroups: Record<string, boolean> = {
    jobs: true,
    company: true,
  };

  constructor() {
    effect(() => {
      const isAuthReady = this.authService.isAuthReady();
      if (!isAuthReady) {
        return;
      }

      const isHirer = this.tokenService.hasAnyRole(['HIRER', 'ROLE_HIRER']);
      if (!isHirer) {
        this.router.navigate(['/recruiter/login'], {
          queryParams: { returnUrl: this.router.url }
        });
        return;
      }

      if (!this.hasLoadedRoles()) {
        this.recruiterAuth.loadHirerRoles();
        this.hasLoadedRoles.set(true);
      }
    });

    effect(() => {
      this.hirerRoles.set(this.recruiterAuth.hirerRoles$());
    });
  }

  ngOnInit(): void {
    this.updateTitle();
    this.updateUserProfile();

    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.updateTitle();
      this.updateUserProfile();
    });
  }

  private updateTitle(): void {
    let route = this.activatedRoute.firstChild;
    while (route?.firstChild) {
      route = route.firstChild;
    }
    this.pageTitle.set(route?.snapshot.data['title'] || 'Overview');
  }

  private updateUserProfile(): void {
    const subject = this.tokenService.getTokenSubject();
    if (subject) {
      this.username.set(subject);
      this.avatarLetter.set(subject.charAt(0).toUpperCase());
    }
  }

  toggleGroup(group: string): void {
    this.openGroups[group] = !this.openGroups[group];
  }

  isGroupOpen(group: string): boolean {
    return !!this.openGroups[group];
  }

  isRouteGroupActive(prefix: string): boolean {
    return this.router.url.startsWith(prefix);
  }

  toggleMobileSidebar(): void {
    this.isMobileSidebarOpen.update((value) => !value);
  }

  closeMobileSidebar(): void {
    this.isMobileSidebarOpen.set(false);
  }

  onLogout(): void {
    this.closeMobileSidebar();
    this.tokenService.clearToken();
    this.authService.logout();
    this.router.navigate(['/recruiter/login']);
  }

  get currentLanguage(): AppLanguage {
    return this.i18nService.currentLanguage;
  }

  switchLanguage(language: AppLanguage): void {
    this.i18nService.setLanguage(language);
  }
}
