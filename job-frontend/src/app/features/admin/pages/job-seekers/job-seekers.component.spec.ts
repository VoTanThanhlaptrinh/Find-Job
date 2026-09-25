import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { of, throwError } from 'rxjs';
import { JobSeekersComponent } from './job-seekers.component';
import { AdminJobSeekersService } from '../../services/admin-job-seekers.service';
import {
  AdminJobSeekerItem,
  AdminJobSeekersMetrics,
  AdminRegionDistribution,
} from '../../services/admin-api.models';

describe('JobSeekersComponent', () => {
  let component: JobSeekersComponent;
  let fixture: ComponentFixture<JobSeekersComponent>;
  let jobSeekersService: AdminJobSeekersService;
  let router: Router;

  const mockMetrics: AdminJobSeekersMetrics = {
    totalSeekers: 1250,
    totalSeekersGrowthPct: 12.5,
    activeLast7Days: 320,
    placedCandidates: 85,
    retentionPct: 92.4,
  };

  const mockJobSeekers: AdminJobSeekerItem[] = [
    {
      id: 'js-1',
      fullName: 'Nguyễn Văn A',
      email: 'nguyenvana@example.com',
      profession: 'Frontend Developer',
      resumeStatus: 'complete',
      lastActiveAt: '2026-03-20T10:30:00Z',
      avatarInitials: 'NA',
    },
    {
      id: 'js-2',
      fullName: 'Trần Thị B',
      email: 'tranthib@example.com',
      profession: 'UI/UX Designer',
      resumeStatus: 'incomplete',
      lastActiveAt: '',
      avatarInitials: 'TB',
    },
    {
      id: 'js-3',
      fullName: 'Lê Hoàng C',
      email: 'lehoangc@example.com',
      profession: 'Backend Developer',
      resumeStatus: 'missing',
      lastActiveAt: '2026-03-22T08:15:00Z',
      avatarInitials: 'LC',
    },
  ];

  const mockRegionDistribution: AdminRegionDistribution = {
    regions: [
      { code: 'APAC', count: 650 },
      { code: 'EU', count: 300 },
      { code: 'NA', count: 200 },
      { code: 'LATAM', count: 60 },
      { code: 'MEA', count: 40 },
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobSeekersComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    jobSeekersService = TestBed.inject(AdminJobSeekersService);
    router = TestBed.inject(Router);

    spyOn(jobSeekersService, 'loadMetrics').and.callFake(() => {});
    spyOn(jobSeekersService, 'loadJobSeekers').and.callFake(() => {});
    spyOn(jobSeekersService, 'loadRegionDistribution').and.callFake(() => {});

    fixture = TestBed.createComponent(JobSeekersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component successfully', () => {
    expect(component).toBeTruthy();
  });

  describe('Component Initialization & Scoped Retries (JS-01, JS-08)', () => {
    it('should trigger loadMetrics and loadRegionDistribution on initialization', () => {
      expect(jobSeekersService.loadMetrics).toHaveBeenCalled();
      expect(jobSeekersService.loadRegionDistribution).toHaveBeenCalled();
    });

    it('should retry metrics reload via retryMetrics()', () => {
      component.retryMetrics();
      expect(jobSeekersService.loadMetrics).toHaveBeenCalledTimes(2);
    });

    it('should retry job seekers list reload via retryList()', () => {
      component.retryList();
      expect(jobSeekersService.loadJobSeekers).toHaveBeenCalled();
    });

    it('should retry region distribution reload via retryRegions()', () => {
      component.retryRegions();
      expect(jobSeekersService.loadRegionDistribution).toHaveBeenCalledTimes(2);
    });
  });

  describe('Search & Filter Debounce and Synchronization (JS-09, JS-10)', () => {
    it('should debounce search input and trigger updateQuery with page reset', fakeAsync(() => {
      spyOn(jobSeekersService, 'updateQuery');
      const inputEvent = { target: { value: 'Nguyễn' } } as unknown as Event;

      component.onSearchInput(inputEvent);
      expect(jobSeekersService.updateQuery).not.toHaveBeenCalled();

      tick(300);
      expect(component.currentPage).toBe(1);
      expect(component.searchTerm).toBe('Nguyễn');
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: 'Nguyễn',
      });
    }));

    it('should clear search term and trigger immediate search update', fakeAsync(() => {
      spyOn(jobSeekersService, 'updateQuery');
      component.searchTerm = 'Developer';

      component.clearSearch();
      expect(component.searchTerm).toBe('');

      tick(300);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: undefined,
      });
    }));

    it('should update resume status filter, reset page and updateQuery', () => {
      spyOn(jobSeekersService, 'updateQuery');
      const selectEvent = { target: { value: 'complete' } } as unknown as Event;

      component.currentPage = 3;
      component.onResumeStatusChange(selectEvent);

      expect(component.selectedResumeStatus).toBe('complete');
      expect(component.currentPage).toBe(1);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        resumeStatus: 'complete',
      });
    });

    it('should clear resume status filter via clearResumeStatusFilter()', () => {
      spyOn(jobSeekersService, 'updateQuery');
      component.selectedResumeStatus = 'incomplete';
      component.currentPage = 2;

      component.clearResumeStatusFilter();

      expect(component.selectedResumeStatus).toBe('');
      expect(component.currentPage).toBe(1);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        resumeStatus: undefined,
      });
    });

    it('should detect active filters and clearFilters cleanly', () => {
      expect(component.hasActiveFilters).toBeFalse();

      component.searchTerm = 'Frontend';
      expect(component.hasActiveFilters).toBeTrue();

      spyOn(jobSeekersService, 'updateQuery');
      component.clearFilters();

      expect(component.searchTerm).toBe('');
      expect(component.selectedResumeStatus).toBe('');
      expect(component.currentPage).toBe(1);
      expect(component.hasActiveFilters).toBeFalse();
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        search: undefined,
        resumeStatus: undefined,
      });
    });

    it('should change page size and reset page to 1', () => {
      spyOn(jobSeekersService, 'updateQuery');
      const selectEvent = { target: { value: '20' } } as unknown as Event;

      component.currentPage = 3;
      component.onPageSizeChange(selectEvent);

      expect(component.pageSize).toBe(20);
      expect(component.currentPage).toBe(1);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({
        page: 1,
        pageSize: 20,
      });
    });

    it('should navigate through pages correctly', () => {
      spyOn(jobSeekersService, 'updateQuery');
      component.totalItems = 50;
      component.pageSize = 10;
      component.currentPage = 2;

      expect(component.totalPages).toBe(5);

      component.goNextPage();
      expect(component.currentPage).toBe(3);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({ page: 3 });

      component.goPrevPage();
      expect(component.currentPage).toBe(2);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({ page: 2 });

      component.goLastPage();
      expect(component.currentPage).toBe(5);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({ page: 5 });

      component.goFirstPage();
      expect(component.currentPage).toBe(1);
      expect(jobSeekersService.updateQuery).toHaveBeenCalledWith({ page: 1 });
    });
  });

  describe('Status Badges, Date Formats & Presentation Helpers (JS-14, JS-15)', () => {
    it('should map resume status to semantic badge tokens', () => {
      const completeBadge = component.getResumeBadge('complete');
      expect(completeBadge.label).toBe('Hoàn chỉnh');
      expect(completeBadge.badgeClass).toContain('bg-emerald-50');

      const incompleteBadge = component.getResumeBadge('incomplete');
      expect(incompleteBadge.label).toBe('Chưa hoàn chỉnh');
      expect(incompleteBadge.badgeClass).toContain('bg-amber-50');

      const missingBadge = component.getResumeBadge('missing');
      expect(missingBadge.label).toBe('Thiếu CV');
      expect(missingBadge.badgeClass).toContain('bg-rose-50');

      const unknownBadge = component.getResumeBadge('other');
      expect(unknownBadge.label).toBe('other');
      expect(unknownBadge.badgeClass).toContain('bg-slate-100');
    });

    it('should format last-active timestamps and return fallback for null/empty', () => {
      expect(component.formatDate(null)).toBe('Chưa có dữ liệu');
      expect(component.formatDate('')).toBe('Chưa có dữ liệu');

      const formatted = component.formatDate('2026-03-20T10:30:00Z');
      expect(formatted).toContain('2026');
      expect(formatted).not.toBe('Vừa xong');
    });

    it('should compute candidate initials accurately', () => {
      expect(component.getInitials('Nguyễn Văn A')).toBe('NA');
      expect(component.getInitials('John')).toBe('JO');
      expect(component.getInitials('Trần Thị B', 'TTB')).toBe('TTB');
      expect(component.getInitials(undefined, undefined)).toBe('UV');
    });

    it('should compute region names, colors, bar widths, and percentages', () => {
      expect(component.getRegionName('APAC')).toBe('Châu Á - TBD (APAC)');
      expect(component.getRegionName('EU')).toBe('Châu Âu (EU)');
      expect(component.getRegionName('UNKNOWN')).toBe('UNKNOWN');

      expect(component.getRegionColorClass('APAC')).toContain('emerald');
      expect(component.getRegionColorClass('NA')).toContain('blue');

      component.regionDistribution = mockRegionDistribution;
      expect(component.getTotalRegionCount()).toBe(1250);
      expect(component.getRegionPercentage(650)).toBe(52);
      expect(component.getRegionBarWidth(650)).toBe(100);
      expect(component.getRegionBarWidth(325)).toBe(50);
    });
  });

  describe('Create Job Seeker Dialog & Unsaved Changes (JS-04, JS-05, JS-06, JS-07)', () => {
    it('should open create dialog and reset form', () => {
      const triggerBtn = document.createElement('button');
      component.openCreateDialog(triggerBtn);

      expect(component.isCreateDialogOpen).toBeTrue();
      expect(component.createError).toBeNull();
      expect(component.createJobSeekerForm.getRawValue()).toEqual({
        fullName: '',
        email: '',
        profession: '',
        resumeUrl: '',
      });
    });

    it('should mark fields as touched if form is invalid on submit', () => {
      component.isCreateDialogOpen = true;
      component.submitCreateJobSeeker();

      expect(component.createJobSeekerForm.invalid).toBeTrue();
      expect(component.isCreateFieldInvalid('fullName')).toBeTrue();
      expect(component.isCreateFieldInvalid('email')).toBeTrue();
      expect(component.isCreateFieldInvalid('profession')).toBeTrue();
      expect(component.isCreateFieldInvalid('resumeUrl')).toBeTrue();
    });

    it('should validate URL format for resumeUrl', () => {
      const urlControl = component.createJobSeekerForm.controls.resumeUrl;

      urlControl.setValue('invalid-url');
      expect(urlControl.invalid).toBeTrue();

      urlControl.setValue('ftp://example.com/cv.pdf');
      expect(urlControl.invalid).toBeTrue();

      urlControl.setValue('https://example.com/cv.pdf');
      expect(urlControl.valid).toBeTrue();

      urlControl.setValue('http://example.com/cv.pdf');
      expect(urlControl.valid).toBeTrue();
    });

    it('should successfully submit valid form and close dialog', () => {
      spyOn(jobSeekersService, 'createJobSeeker').and.returnValue(
        of({ id: 'cand-123', created: true })
      );

      component.openCreateDialog();
      component.createJobSeekerForm.setValue({
        fullName: 'Nguyễn Văn Test',
        email: 'test@example.com',
        profession: 'DevOps Engineer',
        resumeUrl: 'https://example.com/resume.pdf',
      });

      component.submitCreateJobSeeker();

      expect(jobSeekersService.createJobSeeker).toHaveBeenCalledWith({
        fullName: 'Nguyễn Văn Test',
        email: 'test@example.com',
        profession: 'DevOps Engineer',
        resumeUrl: 'https://example.com/resume.pdf',
      });
      expect(component.isCreateDialogOpen).toBeFalse();
    });

    it('should display error message on create failure and keep dialog open', () => {
      spyOn(jobSeekersService, 'createJobSeeker').and.returnValue(
        throwError(() => ({ error: { message: 'Email đã tồn tại' } }))
      );

      component.openCreateDialog();
      component.createJobSeekerForm.setValue({
        fullName: 'Nguyễn Văn Test',
        email: 'existing@example.com',
        profession: 'Tester',
        resumeUrl: 'https://example.com/cv.pdf',
      });

      component.submitCreateJobSeeker();

      expect(component.isCreateDialogOpen).toBeTrue();
      expect(component.createError).toBe('Email đã tồn tại');
    });

    it('should trigger discard confirmation if form is dirty when closing', () => {
      component.openCreateDialog();
      component.createJobSeekerForm.controls.fullName.setValue('Draft Name');
      component.createJobSeekerForm.controls.fullName.markAsDirty();

      component.attemptCloseCreateDialog();
      expect(component.isConfirmDiscardOpen).toBeTrue();
      expect(component.isCreateDialogOpen).toBeTrue();

      component.cancelDiscardChanges();
      expect(component.isConfirmDiscardOpen).toBeFalse();
      expect(component.isCreateDialogOpen).toBeTrue();

      component.confirmDiscardChanges();
      expect(component.isConfirmDiscardOpen).toBeFalse();
      expect(component.isCreateDialogOpen).toBeFalse();
    });

    it('should close immediately without confirmation if form is clean', () => {
      component.openCreateDialog();
      expect(component.createJobSeekerForm.dirty).toBeFalse();

      component.attemptCloseCreateDialog();
      expect(component.isConfirmDiscardOpen).toBeFalse();
      expect(component.isCreateDialogOpen).toBeFalse();
    });

    it('should handle Escape key to close dialog or cancel discard popup', () => {
      component.openCreateDialog();
      component.createJobSeekerForm.controls.fullName.setValue('Draft Name');
      component.createJobSeekerForm.controls.fullName.markAsDirty();

      // Escape triggers attemptCloseCreateDialog -> opens confirm popup
      component.handleEscape();
      expect(component.isConfirmDiscardOpen).toBeTrue();

      // Second Escape cancels the confirm popup
      component.handleEscape();
      expect(component.isConfirmDiscardOpen).toBeFalse();
      expect(component.isCreateDialogOpen).toBeTrue();

      // Clean form Escape closes the dialog directly
      component.createJobSeekerForm.markAsPristine();
      component.handleEscape();
      expect(component.isCreateDialogOpen).toBeFalse();
    });
  });
});
