import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  ElementRef,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser, DOCUMENT } from '@angular/common';
import { Router, RouterModule, NavigationEnd, ActivatedRoute } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { AdminAuthService } from '../../services/admin-auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { AdminNavigationGroup } from '../../services/admin-api.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly adminAuthService = inject(AdminAuthService);
  private readonly tokenService = inject(TokenService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly elementRef = inject(ElementRef);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private routerSub?: Subscription;

  // Breadcrumb & Title metadata dynamically updated from route (DS-04)
  currentTitle = 'Tổng quan hệ thống';
  currentBreadcrumb = 'Tổng quan';

  // Responsive shell state (DS-09, DS-10)
  isMobileDrawerOpen = false;
  isCompactSidebar = false;
  isProfileMenuOpen = false;
  isNotificationOpen = false;
  isLoggingOut = false;

  private menuButtonTrigger: HTMLElement | null = null;

  // Real admin user profile from TokenService (DS-02)
  adminEmail = 'admin@joblisting.vn';
  adminRole = 'Quản trị viên';
  adminInitials = 'AD';

  // Grouped Information Architecture Navigation (DS-05)
  readonly navigationGroups: AdminNavigationGroup[] = [
    {
      id: 'overview',
      label: 'TỔNG QUAN',
      items: [
        {
          label: 'Tổng quan',
          route: '/admin/dashboard',
          icon: 'dashboard',
          capability: 'available',
        },
      ],
    },
    {
      id: 'user-operations',
      label: 'VẬN HÀNH NGƯỜI DÙNG',
      items: [
        {
          label: 'Nhà tuyển dụng',
          route: '/admin/employers',
          icon: 'employers',
          capability: 'available',
        },
        {
          label: 'Người tìm việc',
          route: '/admin/job-seekers',
          icon: 'job-seekers',
          capability: 'available',
        },
      ],
    },
    {
      id: 'recruitment',
      label: 'TUYỂN DỤNG',
      items: [
        {
          label: 'Tin tuyển dụng',
          route: '/admin/jobs',
          icon: 'jobs',
          capability: 'available',
        },
      ],
    },
    {
      id: 'revenue',
      label: 'DOANH THU',
      items: [
        {
          label: 'Gói dịch vụ & Thanh toán',
          route: '/admin/billing',
          icon: 'billing',
          capability: 'available',
          requiredPermission: 'billing.read',
        },
      ],
    },
  ];

  ngOnInit(): void {
    this.extractUserProfile();
    this.updateRouteMetadata();

    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateRouteMetadata();
        this.closeMobileDrawer();
        this.isProfileMenuOpen = false;
        this.isNotificationOpen = false;
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    this.unlockBodyScroll();
  }

  // Profile extraction from JWT (DS-02)
  private extractUserProfile(): void {
    const subject = this.tokenService.getTokenSubject();
    if (subject && subject.trim().length > 0) {
      this.adminEmail = subject;
      const parts = subject.split('@')[0];
      this.adminInitials = (parts.length >= 2 ? parts.slice(0, 2) : parts).toUpperCase();
    } else {
      this.adminEmail = 'Quản trị viên';
      this.adminInitials = 'QT';
    }

    const roles = this.tokenService.getTokenRoles();
    if (roles.includes('SUPER_ADMIN')) {
      this.adminRole = 'Quản trị cấp cao';
    } else if (roles.includes('ADMIN')) {
      this.adminRole = 'Quản trị viên hệ thống';
    } else {
      this.adminRole = 'Quản trị viên';
    }
  }

  // Route metadata updater (DS-04)
  private updateRouteMetadata(): void {
    let currentRoute = this.activatedRoute.root;
    let title = 'Tổng quan hệ thống';
    let breadcrumb = 'Tổng quan';

    while (currentRoute.firstChild) {
      currentRoute = currentRoute.firstChild;
      if (currentRoute.snapshot.data['title']) {
        title = currentRoute.snapshot.data['title'];
      }
      if (currentRoute.snapshot.data['breadcrumb']) {
        breadcrumb = currentRoute.snapshot.data['breadcrumb'];
      }
    }

    this.currentTitle = title;
    this.currentBreadcrumb = breadcrumb;
  }

  // Mobile drawer controls (DS-10)
  toggleMobileDrawer(event?: MouseEvent): void {
    if (event?.currentTarget instanceof HTMLElement) {
      this.menuButtonTrigger = event.currentTarget;
    }
    this.isMobileDrawerOpen = !this.isMobileDrawerOpen;
    if (this.isMobileDrawerOpen) {
      this.lockBodyScroll();
    } else {
      this.unlockBodyScroll();
      this.focusMenuTrigger();
    }
  }

  closeMobileDrawer(): void {
    if (this.isMobileDrawerOpen) {
      this.isMobileDrawerOpen = false;
      this.unlockBodyScroll();
      this.focusMenuTrigger();
    }
  }

  private focusMenuTrigger(): void {
    setTimeout(() => {
      this.menuButtonTrigger?.focus();
    }, 50);
  }

  private lockBodyScroll(): void {
    if (this.isBrowser) {
      this.document.body.style.overflow = 'hidden';
    }
  }

  private unlockBodyScroll(): void {
    if (this.isBrowser) {
      this.document.body.style.overflow = '';
    }
  }

  // Compact sidebar toggle for desktop/tablet (DS-09)
  toggleCompactSidebar(): void {
    this.isCompactSidebar = !this.isCompactSidebar;
  }

  // Profile dropdown menu (DS-15)
  toggleProfileMenu(): void {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
    if (this.isProfileMenuOpen) {
      this.isNotificationOpen = false;
    }
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen = false;
  }

  // Notification menu
  toggleNotification(): void {
    this.isNotificationOpen = !this.isNotificationOpen;
    if (this.isNotificationOpen) {
      this.isProfileMenuOpen = false;
    }
  }

  closeNotification(): void {
    this.isNotificationOpen = false;
  }

  // Logout action (DS-08)
  onLogout(): void {
    if (this.isLoggingOut) return;
    this.isLoggingOut = true;
    this.closeProfileMenu();
    this.closeMobileDrawer();

    this.adminAuthService.logout();
    this.router.navigateByUrl('/admin/login');
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    if (this.isMobileDrawerOpen) {
      this.closeMobileDrawer();
    }
    if (this.isProfileMenuOpen) {
      this.closeProfileMenu();
    }
    if (this.isNotificationOpen) {
      this.closeNotification();
    }
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!this.elementRef.nativeElement.contains(target)) {
      this.closeProfileMenu();
      this.closeNotification();
    }
  }
}
