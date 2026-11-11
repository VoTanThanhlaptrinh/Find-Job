import { CommonModule } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { Component, computed, HostListener, inject, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';

interface InforMenuItem {
  nameKey: string;
  icon: string;
  route: string;
  exact?: boolean;
}

interface InforMenuGroup {
  labelKey: string;
  items: InforMenuItem[];
}

@Component({
  selector: 'app-infor',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent {
  private router = inject(Router);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly userService = inject(UserService);
  isOpen = true;
  isMobileOpen = false;
  isMobileViewport = false;
  readonly candidateName = computed<string | null>(() => {
    const name = this.userService.userDetails()?.fullName;
    return typeof name === 'string' && name.trim() ? name.trim() : null;
  });
  readonly candidateInitial = computed(() => this.candidateName()?.charAt(0).toLocaleUpperCase() ?? '');

  readonly menuGroups: InforMenuGroup[] = [
    {
      labelKey: 'candidateWorkspace.accountGroup',
      items: [
        { nameKey: 'profile.title', icon: 'person', route: '/infor/profile', exact: true },
        { nameKey: 'changePassword.title', icon: 'lock', route: '/infor/change-password' }
      ]
    },
    {
      labelKey: 'candidateWorkspace.jobGroup',
      items: [
        { nameKey: 'cvList.title', icon: 'description', route: '/infor/cv' },
        { nameKey: 'recommendedJobs.title', icon: 'work', route: '/infor/recommended-jobs' },
        { nameKey: 'candidateList.applied', icon: 'history', route: '/infor/history-apply' }
      ]
    }
  ];
  constructor(private auth: AuthService) {
    this.updateViewport();
  }

  @HostListener('window:resize')
  private updateViewport(): void {
    if (!this.isBrowser) {
      return;
    }
    this.isMobileViewport = window.matchMedia('(max-width: 768px)').matches;
    if (!this.isMobileViewport) {
      this.isMobileOpen = false;
    }
  }

  toggleSidebar(): void {
    this.isOpen = !this.isOpen;
  }

  toggleMobileSidebar(): void {
    this.isMobileOpen = !this.isMobileOpen;
  }

  onMenuItemClick(): void {
    this.isMobileOpen = false;
  }

  @HostListener('document:keydown.escape')
  closeMobileSidebar(): void {
    this.isMobileOpen = false;
  }

  isActive(route: string, exact: boolean = false): boolean {
    return this.router.isActive(route, {
      paths: exact ? 'exact' : 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored'
    });
  }
  onLogout() {
    this.closeMobileSidebar();
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
