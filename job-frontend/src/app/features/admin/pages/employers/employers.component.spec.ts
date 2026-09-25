import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { of, throwError } from 'rxjs';
import { EmployersComponent } from './employers.component';
import { AdminEmployersService } from '../../services/admin-employers.service';
import {
  AdminEmployerItem,
  AdminEmployersMetrics,
} from '../../services/admin-api.models';

describe('EmployersComponent', () => {
  let component: EmployersComponent;
  let fixture: ComponentFixture<EmployersComponent>;
  let employersService: AdminEmployersService;
  let router: Router;

  const mockMetrics: AdminEmployersMetrics = {
    totalEmployers: 120,
    totalEmployersGrowthPct: 8.5,
    kycVerified: 110,
    kycVerifiedPct: 91.7,
    pendingKyc: 15,
    suspended: 10,
  };

  const mockEmployers: AdminEmployerItem[] = [
    {
      id: 'emp-1',
      name: 'FPT Software',
      industry: 'Công nghệ thông tin',
      registrationDate: '2026-01-15T08:30:00Z',
      activeJobs: 12,
      kycStatus: 'verified',
      accountStatus: 'active',
      avatarInitials: 'FS',
    },
    {
      id: 'emp-2',
      name: 'Viettel Telecom',
      industry: 'Viễn thông',
      registrationDate: '2026-02-20T10:15:00Z',
      activeJobs: 0,
      kycStatus: 'pending',
      accountStatus: 'suspended',
      avatarInitials: 'VT',
    },
    {
      id: 'emp-3',
      name: 'VNG Corporation',
      industry: 'Game & Internet',
      registrationDate: '2026-03-01T09:00:00Z',
      activeJobs: 5,
      kycStatus: 'rejected',
      accountStatus: 'active',
      avatarInitials: 'VC',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployersComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    employersService = TestBed.inject(AdminEmployersService);
    router = TestBed.inject(Router);

    spyOn(employersService, 'loadMetrics').and.callFake(() => {});
    spyOn(employersService, 'loadEmployers').and.callFake(() => {});

    fixture = TestBed.createComponent(EmployersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component successfully', () => {
    expect(component).toBeTruthy();
  });

  describe('KPI Metrics & Retries (ER-02, ER-13)', () => {
    it('should invoke loadMetrics and updateQuery on initialization', () => {
      expect(employersService.loadMetrics).toHaveBeenCalled();
    });

    it('should trigger retryMetrics to reload metrics', () => {
      component.retryMetrics();
      expect(employersService.loadMetrics).toHaveBeenCalledTimes(2);
    });

    it('should trigger retryList to reload employers table', () => {
      component.retryList();
      expect(employersService.loadEmployers).toHaveBeenCalled();
    });
  });

  describe('Search and Filter Debounce (ER-05, ER-06)', () => {
    it('should debounce search input and trigger updateQuery with page reset', fakeAsync(() => {
      spyOn(employersService, 'updateQuery');
      const inputEvent = { target: { value: 'FPT' } } as unknown as Event;

      component.onSearchInput(inputEvent);
      expect(employersService.updateQuery).not.toHaveBeenCalled();

      tick(300);
      expect(component.currentPage).toBe(1);
      expect(component.searchTerm).toBe('FPT');
      expect(employersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: 'FPT',
      });
    }));

    it('should update KYC filter, reset page and updateQuery', () => {
      spyOn(employersService, 'updateQuery');
      const selectEvent = { target: { value: 'verified' } } as unknown as Event;

      component.currentPage = 3;
      component.onKycStatusChange(selectEvent);

      expect(component.selectedKycStatus).toBe('verified');
      expect(component.currentPage).toBe(1);
      expect(employersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        kycStatus: 'verified',
      });
    });

    it('should update Account status filter, reset page and updateQuery', () => {
      spyOn(employersService, 'updateQuery');
      const selectEvent = { target: { value: 'suspended' } } as unknown as Event;

      component.currentPage = 4;
      component.onAccountStatusChange(selectEvent);

      expect(component.selectedAccountStatus).toBe('suspended');
      expect(component.currentPage).toBe(1);
      expect(employersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        status: 'suspended',
      });
    });

    it('should update page size, reset page and updateQuery', () => {
      spyOn(employersService, 'updateQuery');
      const selectEvent = { target: { value: '25' } } as unknown as Event;

      component.currentPage = 2;
      component.onPageSizeChange(selectEvent);

      expect(component.pageSize).toBe(25);
      expect(component.currentPage).toBe(1);
      expect(employersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        pageSize: 25,
      });
    });

    it('should detect active filters and clearFilters cleanly', () => {
      expect(component.hasActiveFilters).toBeFalse();

      component.searchTerm = 'FPT';
      expect(component.hasActiveFilters).toBeTrue();

      spyOn(employersService, 'updateQuery');
      component.clearFilters();

      expect(component.searchTerm).toBe('');
      expect(component.selectedKycStatus).toBe('');
      expect(component.selectedAccountStatus).toBe('');
      expect(component.currentPage).toBe(1);
      expect(component.hasActiveFilters).toBeFalse();
      expect(employersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: undefined,
        kycStatus: undefined,
        status: undefined,
      });
    });
  });

  describe('Pagination (ER-11)', () => {
    it('should compute totalPages correctly based on totalElements and pageSize', () => {
      component.pageSize = 10;
      component.totalItems = 25;

      expect(component.totalPages).toBe(3);
    });

    it('should compute item range start and end accurately', () => {
      component.pageSize = 10;
      component.currentPage = 2;
      component.totalItems = 25;

      expect(component.pageStartIndex).toBe(11);
      expect(component.pageEndIndex).toBe(20);
    });

    it('should handle zero items in item range', () => {
      component.totalItems = 0;
      expect(component.pageStartIndex).toBe(0);
      expect(component.pageEndIndex).toBe(0);
    });

    it('should change page when valid page is selected and ignore invalid page', () => {
      component.totalItems = 50;
      component.pageSize = 10;
      component.currentPage = 1;
      spyOn(employersService, 'updateQuery');

      component.goToPage(3);
      expect(component.currentPage).toBe(3);
      expect(employersService.updateQuery).toHaveBeenCalledWith({ page: 3 });

      // Out of bounds: should not change
      component.goToPage(0);
      expect(component.currentPage).toBe(3);

      component.goToPage(10);
      expect(component.currentPage).toBe(3);
    });

    it('should navigate with goToPrevPage and goToNextPage', () => {
      component.totalItems = 30;
      component.pageSize = 10;
      component.currentPage = 2;
      spyOn(employersService, 'updateQuery');

      component.goToNextPage();
      expect(component.currentPage).toBe(3);

      component.goToPrevPage();
      expect(component.currentPage).toBe(2);
    });
  });

  describe('Suspend Dialog & Validation (ER-08, ER-10)', () => {
    it('should open suspend dialog and reset reason and error', () => {
      const emp = mockEmployers[0];
      component.openSuspendDialog(emp);

      expect(component.isSuspendDialogOpen).toBeTrue();
      expect(component.employerToSuspend).toEqual(emp);
      expect(component.suspendReason).toBe('');
      expect(component.suspendError).toBeNull();
    });

    it('should close suspend dialog and reset state', () => {
      component.openSuspendDialog(mockEmployers[0]);
      component.closeSuspendDialog();

      expect(component.isSuspendDialogOpen).toBeFalse();
      expect(component.employerToSuspend).toBeNull();
      expect(component.suspendReason).toBe('');
    });

    it('should require non-empty reason before submitting suspend', () => {
      spyOn(employersService, 'updateStatus');
      component.openSuspendDialog(mockEmployers[0]);

      component.suspendReason = '   ';
      component.confirmSuspend();

      expect(component.suspendError).toContain('Vui lòng nhập lý do');
      expect(employersService.updateStatus).not.toHaveBeenCalled();
    });

    it('should submit suspend when reason is valid and close dialog on success', fakeAsync(() => {
      const emp = mockEmployers[0];
      component.openSuspendDialog(emp);
      component.suspendReason = 'Vi phạm chính sách tuyển dụng';

      spyOn(employersService, 'updateStatus').and.returnValue(
        of({ id: emp.id, updated: true })
      );

      component.confirmSuspend();
      tick();

      expect(employersService.updateStatus).toHaveBeenCalledWith(
        emp.id,
        { action: 'suspend', reason: 'Vi phạm chính sách tuyển dụng' },
        emp.name
      );
      expect(component.isSuspendDialogOpen).toBeFalse();
    }));

    it('should display error message and keep dialog open when suspend API fails', fakeAsync(() => {
      const emp = mockEmployers[0];
      component.openSuspendDialog(emp);
      component.suspendReason = 'Vi phạm chính sách';

      spyOn(employersService, 'updateStatus').and.returnValue(
        throwError(() => ({ error: { message: 'Tài khoản không tồn tại hoặc đã bị khóa' } }))
      );

      component.confirmSuspend();
      tick();

      expect(component.isSuspendDialogOpen).toBeTrue();
      expect(component.suspendError).toBe('Tài khoản không tồn tại hoặc đã bị khóa');
    }));
  });

  describe('Restore Confirmation Dialog (ER-09, EC-05)', () => {
    it('should open restore confirmation dialog', () => {
      const suspendedEmp = mockEmployers[1];
      component.openRestoreDialog(suspendedEmp);

      expect(component.isRestoreDialogOpen).toBeTrue();
      expect(component.employerToRestore).toEqual(suspendedEmp);
    });

    it('should close restore dialog', () => {
      component.openRestoreDialog(mockEmployers[1]);
      component.closeRestoreDialog();

      expect(component.isRestoreDialogOpen).toBeFalse();
      expect(component.employerToRestore).toBeNull();
    });

    it('should submit restore with action restore and close dialog on success', fakeAsync(() => {
      const suspendedEmp = mockEmployers[1];
      component.openRestoreDialog(suspendedEmp);

      spyOn(employersService, 'updateStatus').and.returnValue(
        of({ id: suspendedEmp.id, updated: true })
      );

      component.confirmRestore();
      tick();

      expect(employersService.updateStatus).toHaveBeenCalledWith(
        suspendedEmp.id,
        { action: 'restore' },
        suspendedEmp.name
      );
      expect(component.isRestoreDialogOpen).toBeFalse();
    }));
  });

  describe('Detail Drawer (ER-07)', () => {
    it('should open detail drawer, record selectedEmployerId, and call loadEmployerDetail', () => {
      const emp = mockEmployers[0];
      spyOn(employersService, 'loadEmployerDetail');

      component.openDetail(emp);

      expect(component.isDetailDrawerOpen).toBeTrue();
      expect(component.selectedEmployerId).toBe(emp.id);
      expect(employersService.loadEmployerDetail).toHaveBeenCalledWith(emp.id);
    });

    it('should close detail drawer and clear selected employer in service', () => {
      spyOn(employersService, 'clearSelectedEmployer');
      component.openDetail(mockEmployers[0]);

      component.closeDetailDrawer();

      expect(component.isDetailDrawerOpen).toBeFalse();
      expect(component.selectedEmployerId).toBeNull();
      expect(employersService.clearSelectedEmployer).toHaveBeenCalled();
    });

    it('should retry loading detail when retryDetail is invoked', () => {
      spyOn(employersService, 'loadEmployerDetail');
      component.selectedEmployerId = 'emp-123';

      component.retryDetail();
      expect(employersService.loadEmployerDetail).toHaveBeenCalledWith('emp-123');
    });
  });

  describe('Badges and Formatters (ER-14)', () => {
    it('should return correct KYC status badge classes and labels', () => {
      expect(component.getKycLabel('verified')).toBe('Đã xác thực');
      expect(component.getKycLabel('pending')).toBe('Chờ duyệt');
      expect(component.getKycLabel('rejected')).toBe('Bị từ chối');
      expect(component.getKycLabel('unverified')).toBe('unverified');

      expect(component.getKycBadgeClass('verified')).toContain('emerald');
      expect(component.getKycBadgeClass('pending')).toContain('amber');
      expect(component.getKycBadgeClass('rejected')).toContain('rose');
      expect(component.getKycBadgeClass('unverified')).toContain('slate');
    });

    it('should return correct Account status badge classes and labels', () => {
      expect(component.getAccountLabel('active')).toBe('Hoạt động');
      expect(component.getAccountLabel('suspended')).toBe('Đã đình chỉ');
      expect(component.getAccountLabel('unknown')).toBe('unknown');

      expect(component.getAccountBadgeClass('active')).toContain('emerald');
      expect(component.getAccountBadgeClass('suspended')).toContain('rose');
      expect(component.getAccountBadgeClass('unknown')).toContain('slate');
    });

    it('should identify suspended employer correctly', () => {
      expect(component.isEmployerSuspended(mockEmployers[0])).toBeFalse();
      expect(component.isEmployerSuspended(mockEmployers[1])).toBeTrue();
    });

    it('should format dates to Vietnamese locale', () => {
      const formatted = component.formatDate('2026-01-15T08:30:00Z');
      expect(formatted).toContain('15/01/2026');
      expect(component.formatDate(null)).toBe('—');
    });

    it('should track by employer id', () => {
      expect(component.trackByEmployer(0, mockEmployers[0])).toBe(mockEmployers[0].id);
    });
  });

  describe('Keyboard Escape Listener (ER-19)', () => {
    it('should close suspend dialog on escape', () => {
      component.openSuspendDialog(mockEmployers[0]);
      component.handleEscapeKey();
      expect(component.isSuspendDialogOpen).toBeFalse();
    });

    it('should close restore dialog on escape', () => {
      component.openRestoreDialog(mockEmployers[1]);
      component.handleEscapeKey();
      expect(component.isRestoreDialogOpen).toBeFalse();
    });

    it('should close detail drawer on escape', () => {
      component.openDetail(mockEmployers[0]);
      component.handleEscapeKey();
      expect(component.isDetailDrawerOpen).toBeFalse();
    });
  });
});
