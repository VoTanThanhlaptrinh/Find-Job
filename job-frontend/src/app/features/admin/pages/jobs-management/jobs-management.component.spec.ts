import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { of, throwError } from 'rxjs';
import { JobsManagementComponent } from './jobs-management.component';
import { AdminJobsService } from '../../services/admin-jobs.service';
import { CategoryService } from '../../../../core/services/category.service';
import {
  AdminJobItem,
  AdminJobsMetrics,
} from '../../services/admin-api.models';
import { Category } from '../../../../shared/models/category.model';

describe('JobsManagementComponent', () => {
  let component: JobsManagementComponent;
  let fixture: ComponentFixture<JobsManagementComponent>;
  let jobsService: AdminJobsService;
  let categoryService: CategoryService;
  let router: Router;

  const mockMetrics: AdminJobsMetrics = {
    livePostings: 45,
    livePostingsGrowthPct: 15.2,
    pendingReview: 8,
    totalApplicants: 320,
    avgTimeToHireDays: 14,
  };

  const mockCategories: Category[] = [
    { id: 1, name: 'Công nghệ thông tin' },
    { id: 2, name: 'Marketing' },
    { id: 3, name: 'Thiết kế đồ họa' },
  ];

  const mockJobs: AdminJobItem[] = [
    {
      id: 'job-1',
      title: 'Senior Java Developer',
      company: 'FPT Software',
      location: 'Hà Nội',
      category: 'Công nghệ thông tin',
      applications: 24,
      newApplicationsToday: 3,
      status: 'active',
      expiryDate: '2026-05-01T23:59:59Z',
    },
    {
      id: 'job-2',
      title: 'UI/UX Designer',
      company: 'VNG Corporation',
      location: 'TP. Hồ Chí Minh',
      category: 'Thiết kế đồ họa',
      applications: 10,
      newApplicationsToday: 0,
      status: 'pending',
      expiryDate: '2026-04-15T18:00:00Z',
    },
    {
      id: 'job-3',
      title: 'Marketing Specialist',
      company: 'Viettel Telecom',
      location: 'Đà Nẵng',
      category: 'Marketing',
      applications: 5,
      newApplicationsToday: 1,
      status: 'closed',
      expiryDate: '2026-03-01T12:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobsManagementComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    jobsService = TestBed.inject(AdminJobsService);
    categoryService = TestBed.inject(CategoryService);
    router = TestBed.inject(Router);

    spyOn(jobsService, 'loadMetrics').and.callFake(() => {});
    spyOn(jobsService, 'loadJobs').and.callFake(() => {});
    spyOn(categoryService, 'loadCategories').and.callFake(() => {});

    fixture = TestBed.createComponent(JobsManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component successfully', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization & Scoped Retries (JM-01, JM-08)', () => {
    it('should trigger loadMetrics, loadJobs and loadCategories on init', () => {
      expect(jobsService.loadMetrics).toHaveBeenCalled();
      expect(categoryService.loadCategories).toHaveBeenCalled();
    });

    it('should retry metrics reload via retryMetrics()', () => {
      component.retryMetrics();
      expect(jobsService.loadMetrics).toHaveBeenCalledTimes(2);
    });

    it('should retry jobs list reload via retryList()', () => {
      component.retryList();
      expect(jobsService.loadJobs).toHaveBeenCalled();
    });
  });

  describe('Search & Filter Debounce and Synchronization (JM-09)', () => {
    it('should debounce search input and trigger updateQuery with page and selection reset', fakeAsync(() => {
      spyOn(jobsService, 'updateQuery');
      component.selectedJobIds.add('job-1');

      const inputEvent = { target: { value: 'Java' } } as unknown as Event;
      component.onSearch(inputEvent);
      expect(jobsService.updateQuery).not.toHaveBeenCalled();

      tick(300);
      expect(component.currentPage).toBe(1);
      expect(component.searchTerm).toBe('Java');
      expect(component.selectedJobsCount).toBe(0);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: 'Java',
      });
    }));

    it('should clear search term and trigger immediate search update', fakeAsync(() => {
      spyOn(jobsService, 'updateQuery');
      component.searchTerm = 'Designer';

      component.clearSearch();
      expect(component.searchTerm).toBe('');

      tick(300);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: undefined,
      });
    }));

    it('should update category filter, reset page and selection', () => {
      spyOn(jobsService, 'updateQuery');
      component.selectedJobIds.add('job-2');
      const selectEvent = { target: { value: 'Marketing' } } as unknown as Event;

      component.currentPage = 4;
      component.onCategoryChange(selectEvent);

      expect(component.selectedCategory).toBe('Marketing');
      expect(component.currentPage).toBe(1);
      expect(component.selectedJobsCount).toBe(0);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        category: 'Marketing',
      });
    });

    it('should clear category filter via clearCategoryFilter()', () => {
      spyOn(jobsService, 'updateQuery');
      component.selectedCategory = 'Design';
      component.currentPage = 2;

      component.clearCategoryFilter();

      expect(component.selectedCategory).toBe('');
      expect(component.currentPage).toBe(1);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        category: undefined,
      });
    });

    it('should update status filter, reset page and selection', () => {
      spyOn(jobsService, 'updateQuery');
      component.selectedJobIds.add('job-3');
      const selectEvent = { target: { value: 'pending' } } as unknown as Event;

      component.currentPage = 3;
      component.onStatusFilterChange(selectEvent);

      expect(component.selectedStatus).toBe('pending');
      expect(component.currentPage).toBe(1);
      expect(component.selectedJobsCount).toBe(0);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        status: 'pending',
      });
    });

    it('should clear status filter via clearStatusFilter()', () => {
      spyOn(jobsService, 'updateQuery');
      component.selectedStatus = 'active';

      component.clearStatusFilter();

      expect(component.selectedStatus).toBe('');
      expect(component.currentPage).toBe(1);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        status: undefined,
      });
    });

    it('should detect active filters and clearFilters cleanly', () => {
      expect(component.hasActiveFilters).toBeFalse();

      component.searchTerm = 'FPT';
      expect(component.hasActiveFilters).toBeTrue();

      spyOn(jobsService, 'updateQuery');
      component.selectedJobIds.add('job-1');
      component.clearFilters();

      expect(component.searchTerm).toBe('');
      expect(component.selectedCategory).toBe('');
      expect(component.selectedStatus).toBe('');
      expect(component.currentPage).toBe(1);
      expect(component.selectedJobsCount).toBe(0);
      expect(component.hasActiveFilters).toBeFalse();
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: undefined,
        category: undefined,
        status: undefined,
      });
    });

    it('should change page size and reset page and selection', () => {
      spyOn(jobsService, 'updateQuery');
      component.selectedJobIds.add('job-1');
      const selectEvent = { target: { value: '20' } } as unknown as Event;

      component.currentPage = 2;
      component.onPageSizeChange(selectEvent);

      expect(component.pageSize).toBe(20);
      expect(component.currentPage).toBe(1);
      expect(component.selectedJobsCount).toBe(0);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
      });
    });

    it('should navigate through pages correctly', () => {
      spyOn(jobsService, 'updateQuery');
      component.totalItems = 30;
      component.pageSize = 10;
      component.currentPage = 1;

      expect(component.totalPages).toBe(3);

      component.goNextPage();
      expect(component.currentPage).toBe(2);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({ page: 2 });

      component.goPrevPage();
      expect(component.currentPage).toBe(1);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({ page: 1 });

      component.goLastPage();
      expect(component.currentPage).toBe(3);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({ page: 3 });

      component.goFirstPage();
      expect(component.currentPage).toBe(1);
      expect(jobsService.updateQuery).toHaveBeenCalledWith({ page: 1 });
    });
  });

  describe('Selection & Bulk Action State (JM-13, JM-14, JM-15, JM-19)', () => {
    beforeEach(() => {
      component.jobs = mockJobs;
    });

    it('should toggle select all on the current page correctly', () => {
      expect(component.areAllJobsSelected).toBeFalse();
      expect(component.isIndeterminate).toBeFalse();

      component.toggleSelectAll(true);
      expect(component.selectedJobsCount).toBe(3);
      expect(component.areAllJobsSelected).toBeTrue();
      expect(component.isIndeterminate).toBeFalse();

      component.toggleSelectAll(false);
      expect(component.selectedJobsCount).toBe(0);
      expect(component.areAllJobsSelected).toBeFalse();
    });

    it('should toggle individual job selection and track indeterminate state', () => {
      component.toggleJobSelection('job-1', true);
      expect(component.isJobSelected('job-1')).toBeTrue();
      expect(component.selectedJobsCount).toBe(1);
      expect(component.isIndeterminate).toBeTrue();
      expect(component.areAllJobsSelected).toBeFalse();

      component.toggleJobSelection('job-1', false);
      expect(component.isJobSelected('job-1')).toBeFalse();
      expect(component.selectedJobsCount).toBe(0);
      expect(component.isIndeterminate).toBeFalse();
    });

    it('should open and close bulk action dialog', () => {
      component.selectedJobIds.add('job-1');
      component.openBulkDialog();
      expect(component.isBulkDialogOpen).toBeTrue();

      component.closeBulkDialog();
      expect(component.isBulkDialogOpen).toBeFalse();
    });

    it('should submit bulk action and clear selection on success', () => {
      spyOn(jobsService, 'bulkAction').and.returnValue(
        of({ processed: 2, failed: 0 })
      );

      component.selectedJobIds.add('job-1');
      component.selectedJobIds.add('job-2');
      component.bulkActionForm.controls.action.setValue('activate');

      component.openBulkDialog();
      component.confirmBulkAction();

      expect(jobsService.bulkAction).toHaveBeenCalledWith({
        action: 'activate',
        jobIds: ['job-1', 'job-2'],
      });
      expect(component.selectedJobsCount).toBe(0);
      expect(component.isBulkDialogOpen).toBeFalse();
    });

    it('should handle bulk action failure and keep selection', () => {
      spyOn(jobsService, 'bulkAction').and.returnValue(
        throwError(() => ({ error: { message: 'Lỗi máy chủ khi thao tác hàng loạt' } }))
      );

      component.selectedJobIds.add('job-1');
      component.openBulkDialog();
      component.confirmBulkAction();

      expect(component.isBulkDialogOpen).toBeTrue();
      expect(component.bulkDialogError).toBe('Lỗi máy chủ khi thao tác hàng loạt');
      expect(component.selectedJobsCount).toBe(1);
    });
  });

  describe('Row Status Dialog (JM-11, JM-12)', () => {
    it('should open status dialog with job data', () => {
      const job = mockJobs[0];
      component.openStatusDialog(job, 'suspended');

      expect(component.isStatusDialogOpen).toBeTrue();
      expect(component.jobToUpdateStatus).toEqual(job);
      expect(component.statusChangeForm.controls.status.value).toBe('suspended');
    });

    it('should close status dialog and reset form', () => {
      component.openStatusDialog(mockJobs[0]);
      component.closeStatusDialog();

      expect(component.isStatusDialogOpen).toBeFalse();
      expect(component.jobToUpdateStatus).toBeNull();
    });

    it('should submit status update successfully and close dialog', () => {
      spyOn(jobsService, 'updateJobStatus').and.returnValue(
        of({ id: 'job-1', status: 'closed', updatedAt: '2026-03-25T10:00:00Z' })
      );

      component.openStatusDialog(mockJobs[0]);
      component.statusChangeForm.controls.status.setValue('closed');
      component.confirmStatusChange();

      expect(jobsService.updateJobStatus).toHaveBeenCalledWith('job-1', { status: 'closed' });
      expect(component.isStatusDialogOpen).toBeFalse();
    });
  });

  describe('Create Job Drawer & Unsaved Changes (JM-04, JM-05, JM-06, JM-07)', () => {
    it('should open create drawer and reset form', () => {
      component.openCreateDrawer();

      expect(component.isCreateDrawerOpen).toBeTrue();
      expect(component.createError).toBeNull();
      expect(component.createJobForm.getRawValue()).toEqual({
        title: '',
        companyId: '',
        category: '',
        description: '',
        location: '',
        expiryDate: '',
      });
    });

    it('should mark fields as touched if form is invalid on submit', () => {
      component.isCreateDrawerOpen = true;
      component.submitCreateJob();

      expect(component.createJobForm.invalid).toBeTrue();
      expect(component.isCreateFieldInvalid('title')).toBeTrue();
      expect(component.isCreateFieldInvalid('companyId')).toBeTrue();
      expect(component.isCreateFieldInvalid('category')).toBeTrue();
      expect(component.isCreateFieldInvalid('description')).toBeTrue();
      expect(component.isCreateFieldInvalid('location')).toBeTrue();
      expect(component.isCreateFieldInvalid('expiryDate')).toBeTrue();
    });

    it('should submit valid job payload and close drawer on success', () => {
      spyOn(jobsService, 'createJob').and.returnValue(
        of({ id: 'job-new-123', created: true })
      );

      component.openCreateDrawer();
      component.createJobForm.setValue({
        title: 'Lead Cloud Architect',
        companyId: 'cmp-001',
        category: 'Công nghệ thông tin',
        description: 'Chịu trách nhiệm thiết kế kiến trúc đám mây cho toàn bộ dịch vụ.',
        location: 'TP. Hồ Chí Minh',
        expiryDate: '2026-12-31T23:59',
      });

      component.submitCreateJob();

      expect(jobsService.createJob).toHaveBeenCalledWith({
        title: 'Lead Cloud Architect',
        companyId: 'cmp-001',
        category: 'Công nghệ thông tin',
        description: 'Chịu trách nhiệm thiết kế kiến trúc đám mây cho toàn bộ dịch vụ.',
        location: 'TP. Hồ Chí Minh',
        expiryDate: '2026-12-31T23:59',
      });
      expect(component.isCreateDrawerOpen).toBeFalse();
    });

    it('should display error message on create failure and keep drawer open', () => {
      spyOn(jobsService, 'createJob').and.returnValue(
        throwError(() => ({ error: { message: 'Mã doanh nghiệp không tồn tại' } }))
      );

      component.openCreateDrawer();
      component.createJobForm.setValue({
        title: 'Lead Cloud Architect',
        companyId: 'cmp-999',
        category: 'Công nghệ thông tin',
        description: 'Chịu trách nhiệm thiết kế kiến trúc đám mây cho toàn bộ dịch vụ.',
        location: 'TP. Hồ Chí Minh',
        expiryDate: '2026-12-31T23:59',
      });

      component.submitCreateJob();

      expect(component.isCreateDrawerOpen).toBeTrue();
      expect(component.createError).toBe('Mã doanh nghiệp không tồn tại');
    });

    it('should trigger discard confirmation if drawer form is dirty when closing', () => {
      component.openCreateDrawer();
      component.createJobForm.controls.title.setValue('Draft Job Title');
      component.createJobForm.controls.title.markAsDirty();

      component.attemptCloseCreateDrawer();
      expect(component.isConfirmDiscardCreateOpen).toBeTrue();
      expect(component.isCreateDrawerOpen).toBeTrue();

      component.cancelDiscardCreate();
      expect(component.isConfirmDiscardCreateOpen).toBeFalse();
      expect(component.isCreateDrawerOpen).toBeTrue();

      component.confirmDiscardCreate();
      expect(component.isConfirmDiscardCreateOpen).toBeFalse();
      expect(component.isCreateDrawerOpen).toBeFalse();
    });

    it('should close drawer directly without confirmation if form is pristine', () => {
      component.openCreateDrawer();
      expect(component.createJobForm.dirty).toBeFalse();

      component.attemptCloseCreateDrawer();
      expect(component.isConfirmDiscardCreateOpen).toBeFalse();
      expect(component.isCreateDrawerOpen).toBeFalse();
    });

    it('should handle Escape key to close dialogs or drawer', () => {
      // 1. Bulk dialog escape
      component.isBulkDialogOpen = true;
      component.handleEscape();
      expect(component.isBulkDialogOpen).toBeFalse();

      // 2. Status dialog escape
      component.isStatusDialogOpen = true;
      component.handleEscape();
      expect(component.isStatusDialogOpen).toBeFalse();

      // 3. Create drawer escape with dirty form
      component.openCreateDrawer();
      component.createJobForm.controls.title.setValue('Draft Title');
      component.createJobForm.controls.title.markAsDirty();
      component.handleEscape();
      expect(component.isConfirmDiscardCreateOpen).toBeTrue();

      component.handleEscape();
      expect(component.isConfirmDiscardCreateOpen).toBeFalse();
      expect(component.isCreateDrawerOpen).toBeTrue();

      // 4. Create drawer escape with clean form
      component.createJobForm.markAsPristine();
      component.handleEscape();
      expect(component.isCreateDrawerOpen).toBeFalse();
    });
  });

  describe('Status Badges & Presentation Helpers (JM-17, JM-18)', () => {
    it('should map job status to semantic badge tokens', () => {
      const activeBadge = component.getStatusBadge('active');
      expect(activeBadge.label).toBe('Đang hoạt động');
      expect(activeBadge.badgeClass).toContain('bg-emerald-50');

      const pendingBadge = component.getStatusBadge('pending');
      expect(pendingBadge.label).toBe('Chờ duyệt');
      expect(pendingBadge.badgeClass).toContain('bg-amber-50');

      const expiredBadge = component.getStatusBadge('expired');
      expect(expiredBadge.label).toBe('Hết hạn');
      expect(expiredBadge.badgeClass).toContain('bg-rose-50');

      const closedBadge = component.getStatusBadge('closed');
      expect(closedBadge.label).toBe('Đã đóng');
      expect(closedBadge.badgeClass).toContain('bg-slate-100');

      const suspendedBadge = component.getStatusBadge('suspended');
      expect(suspendedBadge.label).toBe('Đã đình chỉ');
      expect(suspendedBadge.badgeClass).toContain('bg-red-50');

      const unknownBadge = component.getStatusBadge('unknown');
      expect(unknownBadge.label).toBe('unknown');
      expect(unknownBadge.badgeClass).toContain('bg-slate-100');
    });

    it('should translate bulk action labels to Vietnamese', () => {
      expect(component.getBulkActionLabel('activate')).toBe('Kích hoạt');
      expect(component.getBulkActionLabel('suspend')).toBe('Đình chỉ');
      expect(component.getBulkActionLabel('close')).toBe('Đóng tin');
      expect(component.getBulkActionLabel('other')).toBe('other');
    });

    it('should format date strings or return fallback for empty/null', () => {
      expect(component.formatDate(null)).toBe('Chưa có dữ liệu');
      expect(component.formatDate('')).toBe('Chưa có dữ liệu');

      const formatted = component.formatDate('2026-05-01T23:59:59Z');
      expect(formatted).toContain('2026');
    });
  });
});
