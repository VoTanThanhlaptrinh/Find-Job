import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  inject,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, take } from 'rxjs';
import { AdminJobSeekersService } from '../../services/admin-job-seekers.service';
import {
  AdminJobSeekerItem,
  AdminJobSeekerListQuery,
  AdminJobSeekersMetrics,
  AdminRegionDistribution,
} from '../../services/admin-api.models';

@Component({
  selector: 'app-job-seekers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './job-seekers.component.html',
})
export class JobSeekersComponent implements OnInit, OnDestroy {
  private readonly jobSeekersService = inject(AdminJobSeekersService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Search debounce subject
  readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamSub?: Subscription;

  // Typed Reactive Form for Create Job Seeker Dialog
  readonly createJobSeekerForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    profession: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    resumeUrl: ['', [Validators.required, Validators.pattern(/^(https?:\/\/).+/i)]],
  });

  // Typed Data States
  metrics: AdminJobSeekersMetrics | null = null;
  jobSeekers: AdminJobSeekerItem[] = [];
  regionDistribution: AdminRegionDistribution | null = null;

  // Scoped Loading States
  isLoadingMetrics = false;
  isLoadingList = false;
  isLoadingRegions = false;
  isCreating = false;

  // Scoped Error States
  metricsError: string | null = null;
  listError: string | null = null;
  regionError: string | null = null;
  createError: string | null = null;

  // Pagination & Filter States
  totalItems = 0;
  currentPage = 1;
  pageSize = 10;
  searchTerm = '';
  selectedResumeStatus = '';

  // Dialog & Unsaved Changes State
  isCreateDialogOpen = false;
  isConfirmDiscardOpen = false;
  private lastFocusedTrigger: HTMLElement | null = null;

  constructor() {
    effect(() => {
      this.metrics = this.jobSeekersService.metrics();
      this.jobSeekers = this.jobSeekersService.jobSeekers();
      this.regionDistribution = this.jobSeekersService.regionDistribution();
      this.totalItems = this.jobSeekersService.totalItems();

      this.isLoadingMetrics = this.jobSeekersService.isLoadingMetrics();
      this.isLoadingList = this.jobSeekersService.isLoadingList();
      this.isLoadingRegions = this.jobSeekersService.isLoadingRegions();
      this.isCreating = this.jobSeekersService.isCreating();

      this.metricsError = this.jobSeekersService.metricsError();
      this.listError = this.jobSeekersService.listError();
      this.regionError = this.jobSeekersService.regionError();

      const query = this.jobSeekersService.currentQuery();
      this.currentPage = query.page ?? 1;
      this.pageSize = query.pageSize ?? 10;
      this.selectedResumeStatus = query.resumeStatus ?? '';
    });
  }

  ngOnInit(): void {
    // 1. Listen for search input debouncing (300ms)
    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        this.currentPage = 1;
        this.searchTerm = term;
        this.updateQueryParamsAndService({
          page: 1,
          search: term.length > 0 ? term : undefined,
        });
      });

    // 2. Read query params on initial load
    this.queryParamSub = this.route.queryParams.pipe(take(1)).subscribe((params) => {
      const page = params['page'] ? Number(params['page']) : 1;
      const pageSize = params['pageSize'] ? Number(params['pageSize']) : 10;
      const search = params['search'] || '';
      const resumeStatus = params['resumeStatus'] || '';

      this.currentPage = isNaN(page) || page < 1 ? 1 : page;
      this.pageSize = [10, 20, 50].includes(pageSize) ? pageSize : 10;
      this.searchTerm = search;
      this.selectedResumeStatus = resumeStatus;

      this.jobSeekersService.updateQuery({
        page: this.currentPage,
        pageSize: this.pageSize,
        search: search.length > 0 ? search : undefined,
        resumeStatus: resumeStatus.length > 0 ? resumeStatus : undefined,
      });
    });

    // 3. Load Metrics and Region Distribution
    this.jobSeekersService.loadMetrics();
    this.jobSeekersService.loadRegionDistribution();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.queryParamSub?.unsubscribe();
  }

  // --- Keyboard navigation & Dialog Listeners ---
  @HostListener('keydown.escape')
  handleEscape(): void {
    if (this.isConfirmDiscardOpen) {
      this.cancelDiscardChanges();
    } else if (this.isCreateDialogOpen) {
      this.attemptCloseCreateDialog();
    }
  }

  // --- Computed UI Helpers ---
  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
  }

  get hasActiveFilters(): boolean {
    return !!(this.searchTerm.trim() || this.selectedResumeStatus);
  }

  getStartIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  getEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  // --- Search & Filter Handlers ---
  onSearchInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.searchSubject.next(raw);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.searchSubject.next('');
  }

  onResumeStatusChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.selectedResumeStatus = val;
    this.currentPage = 1;
    this.updateQueryParamsAndService({
      page: 1,
      resumeStatus: val.length > 0 ? val : undefined,
    });
  }

  clearResumeStatusFilter(): void {
    this.selectedResumeStatus = '';
    this.currentPage = 1;
    this.updateQueryParamsAndService({
      page: 1,
      resumeStatus: undefined,
    });
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedResumeStatus = '';
    this.currentPage = 1;
    this.updateQueryParamsAndService({
      page: 1,
      search: undefined,
      resumeStatus: undefined,
    });
  }

  // --- Pagination Handlers ---
  onPageSizeChange(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);
    this.pageSize = size;
    this.currentPage = 1;
    this.updateQueryParamsAndService({
      page: 1,
      pageSize: size,
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) {
      return;
    }
    this.currentPage = page;
    this.updateQueryParamsAndService({ page });
  }

  goFirstPage(): void {
    this.goToPage(1);
  }

  goPrevPage(): void {
    this.goToPage(this.currentPage - 1);
  }

  goNextPage(): void {
    this.goToPage(this.currentPage + 1);
  }

  goLastPage(): void {
    this.goToPage(this.totalPages);
  }

  // --- Scoped Retry Handlers ---
  retryMetrics(): void {
    this.jobSeekersService.loadMetrics();
  }

  retryList(): void {
    this.jobSeekersService.loadJobSeekers();
  }

  retryRegions(): void {
    this.jobSeekersService.loadRegionDistribution();
  }

  // --- Create Job Seeker Dialog Flow ---
  openCreateDialog(trigger?: HTMLElement): void {
    this.lastFocusedTrigger = trigger || (document.activeElement as HTMLElement);
    this.createError = null;
    this.createJobSeekerForm.reset({
      fullName: '',
      email: '',
      profession: '',
      resumeUrl: '',
    });
    this.isCreateDialogOpen = true;
  }

  attemptCloseCreateDialog(): void {
    if (this.createJobSeekerForm.dirty && !this.isCreating) {
      this.isConfirmDiscardOpen = true;
    } else {
      this.closeCreateDialog();
    }
  }

  confirmDiscardChanges(): void {
    this.isConfirmDiscardOpen = false;
    this.closeCreateDialog();
  }

  cancelDiscardChanges(): void {
    this.isConfirmDiscardOpen = false;
  }

  closeCreateDialog(): void {
    this.isCreateDialogOpen = false;
    this.isConfirmDiscardOpen = false;
    this.createError = null;
    this.createJobSeekerForm.reset({
      fullName: '',
      email: '',
      profession: '',
      resumeUrl: '',
    });
    if (this.lastFocusedTrigger) {
      this.lastFocusedTrigger.focus();
    }
  }

  isCreateFieldInvalid(
    controlName: 'fullName' | 'email' | 'profession' | 'resumeUrl'
  ): boolean {
    const control = this.createJobSeekerForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  submitCreateJobSeeker(): void {
    if (this.createJobSeekerForm.invalid) {
      this.createJobSeekerForm.markAllAsTouched();
      return;
    }

    this.createError = null;
    const payload = this.createJobSeekerForm.getRawValue();

    this.jobSeekersService
      .createJobSeeker(payload)
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.closeCreateDialog();
        },
        error: (err) => {
          this.createError =
            err?.error?.message ||
            'Không thể tạo hồ sơ người tìm việc. Vui lòng kiểm tra lại thông tin và thử lại.';
        },
      });
  }

  // --- Presentation Helpers ---
  getResumeBadge(status: string): {
    label: string;
    badgeClass: string;
    dotClass: string;
    icon: string;
  } {
    switch (status?.toLowerCase()) {
      case 'complete':
        return {
          label: 'Hoàn chỉnh',
          badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200/60',
          dotClass: 'bg-emerald-500',
          icon: 'check_circle',
        };
      case 'incomplete':
        return {
          label: 'Chưa hoàn chỉnh',
          badgeClass: 'bg-amber-50 text-amber-700 border-amber-200/60',
          dotClass: 'bg-amber-500',
          icon: 'help',
        };
      case 'missing':
        return {
          label: 'Thiếu CV',
          badgeClass: 'bg-rose-50 text-rose-700 border-rose-200/60',
          dotClass: 'bg-rose-500',
          icon: 'cancel',
        };
      default:
        return {
          label: status || 'Chưa xác định',
          badgeClass: 'bg-slate-100 text-slate-600 border-slate-200',
          dotClass: 'bg-slate-400',
          icon: 'info',
        };
    }
  }

  formatDate(dateStr?: string | null): string {
    if (!dateStr) {
      return 'Chưa có dữ liệu';
    }
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) {
        return dateStr;
      }
      return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(d);
    } catch {
      return dateStr;
    }
  }

  getInitials(name?: string, avatarInitials?: string): string {
    if (avatarInitials && avatarInitials.trim()) {
      return avatarInitials.trim().toUpperCase();
    }
    if (!name) return 'UV';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  getRegionName(code: string): string {
    const names: Record<string, string> = {
      NA: 'Bắc Mỹ (NA)',
      EU: 'Châu Âu (EU)',
      APAC: 'Châu Á - TBD (APAC)',
      LATAM: 'Mỹ Latinh (LATAM)',
      MEA: 'Trung Đông & Châu Phi (MEA)',
    };
    return names[code] || code;
  }

  getRegionColorClass(code: string): string {
    const colors: Record<string, string> = {
      NA: 'bg-blue-500',
      EU: 'bg-indigo-500',
      APAC: 'bg-emerald-500',
      LATAM: 'bg-amber-500',
      MEA: 'bg-purple-500',
    };
    return colors[code] || 'bg-sky-500';
  }

  getRegionBarWidth(count: number): number {
    if (!this.regionDistribution?.regions?.length) return 0;
    const maxCount = Math.max(...this.regionDistribution.regions.map((r) => r.count), 1);
    return Math.max(Math.round((count / maxCount) * 100), 4);
  }

  getTotalRegionCount(): number {
    if (!this.regionDistribution?.regions?.length) return 0;
    return this.regionDistribution.regions.reduce((acc, r) => acc + r.count, 0);
  }

  getRegionPercentage(count: number): number {
    const total = this.getTotalRegionCount();
    if (total <= 0) return 0;
    return Math.round((count / total) * 100);
  }

  trackBySeeker(_: number, item: AdminJobSeekerItem): string {
    return item.id;
  }

  // --- Internal Query Synchronizer ---
  private updateQueryParamsAndService(patch: Partial<AdminJobSeekerListQuery>): void {
    const currentQuery = this.jobSeekersService.currentQuery();
    const newQuery: AdminJobSeekerListQuery = {
      ...currentQuery,
      ...patch,
    };

    // Keep URL clean: only include non-default params
    const queryParams: Record<string, any> = {};
    if (newQuery.page && newQuery.page > 1) queryParams['page'] = newQuery.page;
    if (newQuery.pageSize && newQuery.pageSize !== 10) queryParams['pageSize'] = newQuery.pageSize;
    if (newQuery.search) queryParams['search'] = newQuery.search;
    if (newQuery.resumeStatus) queryParams['resumeStatus'] = newQuery.resumeStatus;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
    });

    this.jobSeekersService.updateQuery(patch);
  }
}
