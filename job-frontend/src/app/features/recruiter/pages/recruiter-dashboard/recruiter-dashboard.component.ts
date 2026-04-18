import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterAuthService } from '../../services/recruiter-auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-recruiter-dashboard',
  imports: [RouterOutlet, RouterLink, TranslatePipe],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrl: './recruiter-dashboard.component.css',
})
export class RecruiterDashboardComponent {
  private readonly router = inject(Router);
  private readonly recruiterAuth = inject(RecruiterAuthService);
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly hasLoadedRoles = signal<boolean>(false);
  readonly hirerRoles = signal<string[]>([]);
  readonly isMobileSidebarOpen = signal<boolean>(false);
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
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/recruiter/login']);
  }
}
