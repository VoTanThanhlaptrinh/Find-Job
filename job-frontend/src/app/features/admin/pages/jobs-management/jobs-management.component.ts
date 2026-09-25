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
import { AdminJobsService } from '../../services/admin-jobs.service';
import { CategoryService } from '../../../../core/services/category.service';
import {
  AdminJobItem,
  AdminJobsListQuery,
  AdminJobsMetrics,
} from '../../services/admin-api.models';
import { Category } from '../../../../shared/models/category.model';

@Component({
  selector: 'app-jobs-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './jobs-management.component.html',
})
export class JobsManagementComponent implements OnInit, OnDestroy {
  private readonly jobsService = inject(AdminJobsService);
  private readonly categoryService = inject(CategoryService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Search debounce subject
  readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private queryParamSub?: Subscription;

  // Typed Reactive Forms
  readonly createJobForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
    companyId: ['', [Validators.required]],
    category: ['', [Validators.required]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    location: ['', [Validators.required]],
    expiryDate: ['', [Validators.required]],
  });

  readonly statusChangeForm = this.fb.nonNullable.group({
    status: ['active', [Validators.required]],
    reason: [''],
  });

  readonly bulkActionForm = this.fb.nonNullable.group({
    action: ['activate', [Validators.required]],
    reason: [''],
  });

  // Selection Set (Local to current page)
  readonly selectedJobIds = new Set<string>();

  // Typed Data States
  metrics: AdminJobsMetrics | null = null;
  jobs: AdminJobItem[] = [];
  categories: Category[] = [];

  // Scoped Loading States
  isLoadingMetrics = false;
  isLoadingList = false;
  isLoadingCategories = false;
  isCreating = false;
  updatingStatusJobId: string | null = null;
  isSubmittingBulk = false;

  // Scoped Error States
  metricsError: string | null = null;
  listError: string | null = null;
  createError: string | null = null;
  statusDialogError: string | null = null;
  bulkDialogError: string | null = null;

  // Pagination & Filter States
  totalItems = 0;
  currentPage = 1;
  pageSize = 10;
  searchTerm = '';
  selectedCategory = '';
  selectedStatus = '';

  // Drawer & Dialog States
  isCreateDrawerOpen = false;
  isConfirmDiscardCreateOpen = false;
  isStatusDialogOpen = false;
  isBulkDialogOpen = false;
  jobToUpdateStatus: AdminJobItem | null = null;
  private lastFocusedTrigger: HTMLElement | null = null;

  constructor() {
    effect(() => {
      this.metrics = this.jobsService.metrics();
      this.jobs = this.jobsService.jobs();
      this.totalItems = this.jobsService.totalItems();

      this.isLoadingMetrics = this.jobsService.isLoadingMetrics();
      this.isLoadingList = this.jobsService.isLoadingList();
      this.isCreating = this.jobsService.isCreating();
      this.updatingStatusJobId = this.jobsService.updatingStatusJobId();

      this.metricsError = this.jobsService.metricsError();
      this.listError = this.jobsService.listError();

      const query = this.jobsService.currentQuery();
      this.currentPage = query.page ?? 1;
      this.pageSize = query.pageSize ?? 10;
      this.selectedCategory = query.category ?? '';
      this.selectedStatus = query.status ?? '';

      // Sync categories from CategoryService
      this.categories = this.categoryService.categories();
      this.isLoadingCategories = this.categoryService.isLoadingCategories();
    });
  }

  ngOnInit(): void {
    // 1. Listen for search input debouncing (300ms)
    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        this.currentPage = 1;
        this.searchTerm = term;
        this.selectedJobIds.clear();
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
      const category = params['category'] || '';
      const status = params['status'] || '';

      this.currentPage = isNaN(page) || page < 1 ? 1 : page;
      this.pageSize = [10, 20, 50].includes(pageSize) ? pageSize : 10;
      this.searchTerm = search;
      this.selectedCategory = category;
      this.selectedStatus = status;

      this.jobsService.updateQuery({
        page: this.currentPage,
        pageSize: this.pageSize,
        search: search.length > 0 ? search : undefined,
        category: category.length > 0 ? category : undefined,
        status: status.length > 0 ? status : undefined,
      });
    });

    // 3. Load Metrics and Categories
    this.jobsService.loadMetrics();
    this.categoryService.loadCategories();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.queryParamSub?.unsubscribe();
  }

  // --- Keyboard & Escape Listener ---
  @HostListener('keydown.escape')
  handleEscape(): void {
    if (this.isConfirmDiscardCreateOpen) {
      this.cancelDiscardCreate();
    } else if (this.isCreateDrawerOpen) {
      this.attemptCloseCreateDrawer();
    } else if (this.isStatusDialogOpen) {
      this.closeStatusDialog();
    } else if (this.isBulkDialogOpen) {
      this.closeBulkDialog();
    }
  }

  // --- Computed UI Helpers ---
  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
  }

  get selectedJobsCount(): number {
    return this.selectedJobIds.size;
  }

  get areAllJobsSelected(): boolean {
    return this.jobs.length > 0 && this.jobs.every((j) => this.selectedJobIds.has(j.id));
  }

  get isIndeterminate(): boolean {
    return this.selectedJobIds.size > 0 && !this.areAllJobsSelected;
  }

  get hasActiveFilters(): boolean {
    return !!(this.searchTerm.trim() || this.selectedCategory || this.selectedStatus);
  }

  getStartIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  getEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  // --- Search & Filter Handlers ---
  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.searchSubject.next(raw);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.searchSubject.next('');
  }

  onCategoryChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.selectedCategory = val;
    this.currentPage = 1;
    this.selectedJobIds.clear();
    this.updateQueryParamsAndService({
      page: 1,
      category: val.length > 0 ? val : undefined,
    });
  }

  clearCategoryFilter(): void {
    this.selectedCategory = '';
    this.currentPage = 1;
    this.selectedJobIds.clear();
    this.updateQueryParamsAndService({
      page: 1,
      category: undefined,
    });
  }

  onStatusFilterChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.selectedStatus = val;
    this.currentPage = 1;
    this.selectedJobIds.clear();
    this.updateQueryParamsAndService({
      page: 1,
      status: val.length > 0 ? val : undefined,
    });
  }

  clearStatusFilter(): void {
    this.selectedStatus = '';
    this.currentPage = 1;
    this.selectedJobIds.clear();
    this.updateQueryParamsAndService({
      page: 1,
      status: undefined,
    });
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.selectedStatus = '';
    this.currentPage = 1;
    this.selectedJobIds.clear();
    this.updateQueryParamsAndService({
      page: 1,
      search: undefined,
      category: undefined,
      status: undefined,
    });
  }

  // --- Pagination Handlers ---
  onPageSizeChange(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);
    this.pageSize = size;
    this.currentPage = 1;
    this.selectedJobIds.clear();
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
    this.selectedJobIds.clear();
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

  // --- Scoped Retries ---
  retryMetrics(): void {
    this.jobsService.loadMetrics();
  }

  retryList(): void {
    this.jobsService.loadJobs();
  }

  // --- Selection Handlers (JM-15, JM-19) ---
  toggleSelectAll(checked: boolean): void {
    this.selectedJobIds.clear();
    if (checked) {
      for (const job of this.jobs) {
        this.selectedJobIds.add(job.id);
      }
    }
  }

  toggleJobSelection(jobId: string, checked: boolean): void {
    if (checked) {
      this.selectedJobIds.add(jobId);
    } else {
      this.selectedJobIds.delete(jobId);
    }
  }

  isJobSelected(jobId: string): boolean {
    return this.selectedJobIds.has(jobId);
  }

  clearSelection(): void {
    this.selectedJobIds.clear();
  }

  // --- Create Job Drawer Flow (JM-04, JM-05, JM-06, JM-07) ---
  openCreateDrawer(trigger?: HTMLElement): void {
    this.lastFocusedTrigger = trigger || (document.activeElement as HTMLElement);
    this.createError = null;
    this.createJobForm.reset({
      title: '',
      companyId: '',
      category: '',
      description: '',
      location: '',
      expiryDate: '',
    });
    this.isCreateDrawerOpen = true;
  }

  attemptCloseCreateDrawer(): void {
    if (this.createJobForm.dirty && !this.isCreating) {
      this.isConfirmDiscardCreateOpen = true;
    } else {
      this.closeCreateDrawer();
    }
  }

  confirmDiscardCreate(): void {
    this.isConfirmDiscardCreateOpen = false;
    this.closeCreateDrawer();
  }

  cancelDiscardCreate(): void {
    this.isConfirmDiscardCreateOpen = false;
  }

  closeCreateDrawer(): void {
    this.isCreateDrawerOpen = false;
    this.isConfirmDiscardCreateOpen = false;
    this.createError = null;
    this.createJobForm.reset();
    if (this.lastFocusedTrigger) {
      this.lastFocusedTrigger.focus();
    }
  }

  isCreateFieldInvalid(
    controlName: 'title' | 'companyId' | 'category' | 'description' | 'location' | 'expiryDate'
  ): boolean {
    const control = this.createJobForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  submitCreateJob(): void {
    if (this.createJobForm.invalid) {
      this.createJobForm.markAllAsTouched();
      return;
    }

    this.createError = null;
    const payload = this.createJobForm.getRawValue();

    this.jobsService
      .createJob(payload)
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.closeCreateDrawer();
        },
        error: (err) => {
          this.createError =
            err?.error?.message ||
            'Không thể tạo tin tuyển dụng. Vui lòng kiểm tra lại thông tin và thử lại.';
        },
      });
  }

  // --- Row Status Change Dialog (JM-11, JM-12) ---
  openStatusDialog(job: AdminJobItem, initialStatus?: string): void {
    this.jobToUpdateStatus = job;
    this.statusDialogError = null;
    this.statusChangeForm.setValue({
      status: initialStatus || job.status || 'active',
      reason: '',
    });
    this.isStatusDialogOpen = true;
  }

  closeStatusDialog(): void {
    this.isStatusDialogOpen = false;
    this.jobToUpdateStatus = null;
    this.statusDialogError = null;
    this.statusChangeForm.reset({ status: 'active', reason: '' });
  }

  confirmStatusChange(): void {
    if (!this.jobToUpdateStatus) return;

    const { status, reason } = this.statusChangeForm.getRawValue();
    if (!status || status === this.jobToUpdateStatus.status) {
      this.closeStatusDialog();
      return;
    }

    this.statusDialogError = null;
    this.jobsService
      .updateJobStatus(this.jobToUpdateStatus.id, { status })
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.closeStatusDialog();
        },
        error: (err) => {
          this.statusDialogError =
            err?.error?.message || 'Cập nhật trạng thái tin tuyển dụng thất bại.';
        },
      });
  }

  // --- Bulk Action Confirmation Dialog (JM-13, JM-14) ---
  openBulkDialog(): void {
    if (this.selectedJobsCount === 0) return;
    this.bulkDialogError = null;
    this.isBulkDialogOpen = true;
  }

  closeBulkDialog(): void {
    this.isBulkDialogOpen = false;
    this.bulkDialogError = null;
    this.isSubmittingBulk = false;
  }

  confirmBulkAction(): void {
    if (this.bulkActionForm.invalid || this.selectedJobsCount === 0) return;

    this.isSubmittingBulk = true;
    this.bulkDialogError = null;
    const { action } = this.bulkActionForm.getRawValue();

    this.jobsService
      .bulkAction({
        action,
        jobIds: Array.from(this.selectedJobIds),
      })
      .pipe(take(1))
      .subscribe({
        next: (res) => {
          this.isSubmittingBulk = false;
          this.selectedJobIds.clear();
          this.closeBulkDialog();
        },
        error: (err) => {
          this.isSubmittingBulk = false;
          this.bulkDialogError =
            err?.error?.message || 'Có lỗi xảy ra khi thực hiện thao tác hàng loạt.';
        },
      });
  }

  // --- Presentation Helpers (JM-17, JM-18) ---
  getStatusBadge(status: string): {
    label: string;
    badgeClass: string;
    dotClass: string;
  } {
    switch (status?.toLowerCase()) {
      case 'active':
        return {
          label: 'Đang hoạt động',
          badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200/60',
          dotClass: 'bg-emerald-500',
        };
      case 'pending':
        return {
          label: 'Chờ duyệt',
          badgeClass: 'bg-amber-50 text-amber-700 border-amber-200/60',
          dotClass: 'bg-amber-500',
        };
      case 'expired':
        return {
          label: 'Hết hạn',
          badgeClass: 'bg-rose-50 text-rose-700 border-rose-200/60',
          dotClass: 'bg-rose-500',
        };
      case 'closed':
        return {
          label: 'Đã đóng',
          badgeClass: 'bg-slate-100 text-slate-700 border-slate-200',
          dotClass: 'bg-slate-400',
        };
      case 'suspended':
        return {
          label: 'Đã đình chỉ',
          badgeClass: 'bg-red-50 text-red-700 border-red-200/60',
          dotClass: 'bg-red-500',
        };
      default:
        return {
          label: status || 'Chưa xác định',
          badgeClass: 'bg-slate-100 text-slate-600 border-slate-200',
          dotClass: 'bg-slate-400',
        };
    }
  }

  getBulkActionLabel(action: string): string {
    switch (action) {
      case 'activate':
        return 'Kích hoạt';
      case 'suspend':
        return 'Đình chỉ';
      case 'close':
        return 'Đóng tin';
      default:
        return action;
    }
  }

  formatDate(dateStr?: string | null): string {
    if (!dateStr) return 'Chưa có dữ liệu';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      }).format(d);
    } catch {
      return dateStr;
    }
  }

  trackByJob(_: number, item: AdminJobItem): string {
    return item.id;
  }

  // --- Internal Query Synchronizer ---
  private updateQueryParamsAndService(patch: Partial<AdminJobsListQuery>): void {
    const currentQuery = this.jobsService.currentQuery();
    const newQuery: AdminJobsListQuery = {
      ...currentQuery,
      ...patch,
    };

    const queryParams: Record<string, any> = {};
    if (newQuery.page && newQuery.page > 1) queryParams['page'] = newQuery.page;
    if (newQuery.pageSize && newQuery.pageSize !== 10) queryParams['pageSize'] = newQuery.pageSize;
    if (newQuery.search) queryParams['search'] = newQuery.search;
    if (newQuery.category) queryParams['category'] = newQuery.category;
    if (newQuery.status) queryParams['status'] = newQuery.status;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
    });

    this.jobsService.updateQuery(patch);
  }
}
