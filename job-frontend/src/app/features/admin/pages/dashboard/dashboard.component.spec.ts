import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { Component } from '@angular/core';
import { DashboardComponent } from './dashboard.component';
import { AdminAuthService } from '../../services/admin-auth.service';
import { TokenService } from '../../../../core/services/token.service';

@Component({
  standalone: true,
  template: '<div>Dummy Child Component</div>',
})
class DummyChildComponent {}

describe('DashboardComponent (Admin Shell)', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let adminAuthService: AdminAuthService;
  let tokenService: TokenService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([
          {
            path: 'admin',
            component: DashboardComponent,
            children: [
              {
                path: 'dashboard',
                component: DummyChildComponent,
                data: { title: 'Tổng quan hệ thống', breadcrumb: 'Tổng quan' },
              },
              {
                path: 'employers',
                component: DummyChildComponent,
                data: { title: 'Quản lý nhà tuyển dụng', breadcrumb: 'Nhà tuyển dụng' },
              },
              {
                path: 'billing',
                component: DummyChildComponent,
                data: { title: 'Thanh toán & Gói dịch vụ', breadcrumb: 'Gói dịch vụ' },
              },
            ],
          },
          { path: 'admin/login', component: DummyChildComponent },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    adminAuthService = TestBed.inject(AdminAuthService);
    tokenService = TestBed.inject(TokenService);
    router = TestBed.inject(Router);

    spyOn(tokenService, 'getTokenSubject').and.returnValue('superadmin@joblisting.vn');
    spyOn(tokenService, 'getTokenRoles').and.returnValue(['SUPER_ADMIN']);

    fixture.detectChanges();
  });

  it('should create the Dashboard shell component successfully', () => {
    expect(component).toBeTruthy();
  });

  describe('Branding & Grouped Navigation (DS-01, DS-05)', () => {
    it('should have 4 navigation groups with Vietnamese labels', () => {
      expect(component.navigationGroups.length).toBe(4);
      expect(component.navigationGroups[0].label).toBe('TỔNG QUAN');
      expect(component.navigationGroups[1].label).toBe('VẬN HÀNH NGƯỜI DÙNG');
      expect(component.navigationGroups[2].label).toBe('TUYỂN DỤNG');
      expect(component.navigationGroups[3].label).toBe('DOANH THU');
    });

    it('should contain all 5 available routes', () => {
      const allItems = component.navigationGroups.flatMap((g) => g.items);
      const routes = allItems.map((i) => i.route);

      expect(routes).toContain('/admin/dashboard');
      expect(routes).toContain('/admin/employers');
      expect(routes).toContain('/admin/job-seekers');
      expect(routes).toContain('/admin/jobs');
      expect(routes).toContain('/admin/billing');
    });
  });

  describe('User Profile & Session extraction (DS-02)', () => {
    it('should extract real email and role from TokenService without fake data', () => {
      expect(component.adminEmail).toBe('superadmin@joblisting.vn');
      expect(component.adminRole).toBe('Quản trị cấp cao');
      expect(component.adminInitials).toBe('SU');
    });
  });

  describe('Route dynamic title and breadcrumb (DS-04)', () => {
    it('should update title and breadcrumb when navigating to a child route', fakeAsync(() => {
      router.navigateByUrl('/admin/employers');
      tick();
      fixture.detectChanges();

      expect(component.currentTitle).toBe('Quản lý nhà tuyển dụng');
      expect(component.currentBreadcrumb).toBe('Nhà tuyển dụng');

      router.navigateByUrl('/admin/billing');
      tick();
      fixture.detectChanges();

      expect(component.currentTitle).toBe('Thanh toán & Gói dịch vụ');
      expect(component.currentBreadcrumb).toBe('Gói dịch vụ');
    }));
  });

  describe('Responsive Sidebar & Mobile Drawer (DS-09, DS-10)', () => {
    it('should toggle compact sidebar state', () => {
      expect(component.isCompactSidebar).toBeFalse();
      component.toggleCompactSidebar();
      expect(component.isCompactSidebar).toBeTrue();
      component.toggleCompactSidebar();
      expect(component.isCompactSidebar).toBeFalse();
    });

    it('should toggle and close mobile drawer', () => {
      expect(component.isMobileDrawerOpen).toBeFalse();
      component.toggleMobileDrawer();
      expect(component.isMobileDrawerOpen).toBeTrue();

      component.closeMobileDrawer();
      expect(component.isMobileDrawerOpen).toBeFalse();
    });

    it('should close mobile drawer and dropdowns on Escape key press', () => {
      component.isMobileDrawerOpen = true;
      component.isProfileMenuOpen = true;
      component.isNotificationOpen = true;

      component.handleEscape();

      expect(component.isMobileDrawerOpen).toBeFalse();
      expect(component.isProfileMenuOpen).toBeFalse();
      expect(component.isNotificationOpen).toBeFalse();
    });

    it('should automatically close mobile drawer upon navigation', fakeAsync(() => {
      component.isMobileDrawerOpen = true;
      router.navigateByUrl('/admin/dashboard');
      tick();

      expect(component.isMobileDrawerOpen).toBeFalse();
    }));
  });

  describe('Profile and Notification Popovers (DS-03, DS-15)', () => {
    it('should toggle profile menu and close other popovers', () => {
      component.isNotificationOpen = true;
      component.toggleProfileMenu();

      expect(component.isProfileMenuOpen).toBeTrue();
      expect(component.isNotificationOpen).toBeFalse();
    });

    it('should toggle notification popover and close profile menu', () => {
      component.isProfileMenuOpen = true;
      component.toggleNotification();

      expect(component.isNotificationOpen).toBeTrue();
      expect(component.isProfileMenuOpen).toBeFalse();
    });
  });

  describe('Logout flow (DS-08)', () => {
    it('should invoke adminAuthService.logout and navigate to login', () => {
      spyOn(adminAuthService, 'logout');
      spyOn(router, 'navigateByUrl');

      component.onLogout();

      expect(component.isLoggingOut).toBeTrue();
      expect(adminAuthService.logout).toHaveBeenCalled();
      expect(router.navigateByUrl).toHaveBeenCalledWith('/admin/login');
    });
  });
});
