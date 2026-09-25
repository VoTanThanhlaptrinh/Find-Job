import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  inject,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Subject, Subscription, debounceTime, distinctUntilChanged, take, finalize } from 'rxjs';
import { AdminEmployersService } from '../../services/admin-employers.service';
import {
  AdminEmployerDetail,
  AdminEmployerItem,
  AdminEmployersMetrics,
} from '../../services/admin-api.models';

export type LoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';

@Component({
  selector: 'app-employers',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './employers.component.html',
})
export class EmployersComponent implements OnInit, OnDestroy {
  private readonly employersService = inject(AdminEmployersService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Search debounce subject
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  // Data signals mirror
  metrics: AdminEmployersMetrics | null = null;
  employers: AdminEmployerItem[] = [];
  selectedEmployer: AdminEmployerDetail | null = null;

  // Scoped loading and error states (ER-04)
  isLoadingMetrics = false;
  isLoadingList = false;
  isLoadingDetail = false;
  updatingEmployerId: string | null = null;

  metricsError: string | null = null;
  listError: string | null = null;
  detailError: string | null = null;

  // Pagination & Query Filters
  currentPage = 1;
  totalItems = 0;
  pageSize = 10;
  searchTerm = '';
  selectedKycStatus = '';
  selectedAccountStatus = '';

  // Detail Drawer state (ER-07)
  isDetailDrawerOpen = false;
  selectedEmployerId: string | null = null;
  private lastFocusedTrigger: HTMLElement | null = null;

  // Suspend Dialog state (ER-08, EC-06)
  isSuspendDialogOpen = false;
  employerToSuspend: AdminEmployerItem | null = null;
  suspendReason = '';
  suspendError: string | null = null;
  isSubmittingSuspend = false;

  // Restore Confirmation Dialog state (ER-09, EC-05)
  isRestoreDialogOpen = false;
  employerToRestore: AdminEmployerItem | null = null;
  isSubmittingRestore = false;

  // Export state (ER-12)
  isExporting = false;

  constructor() {
    effect(() => {
      this.metrics = this.employersService.metrics();
      this.employers = this.employersService.employers();
      this.selectedEmployer = this.employersService.selectedEmployer();
      this.totalItems = this.employersService.totalItems();

      this.isLoadingMetrics = this.employersService.isLoadingMetrics();
      this.isLoadingList = this.employersService.isLoadingList();
      this.isLoadingDetail = this.employersService.isLoadingDetail();
      this.updatingEmployerId = this.employersService.updatingEmployerId();

      this.metricsError = this.employersService.metricsError();
      this.listError = this.employersService.listError();
      this.detailError = this.employersService.detailError();

      const query = this.employersService.currentQuery();
      this.currentPage = query.page ?? 1;
      this.pageSize = query.pageSize ?? 10;
      this.selectedKycStatus = query.kycStatus ?? '';
      this.selectedAccountStatus = query.status ?? '';
      this.searchTerm = query.search ?? '';
    });
  }

  ngOnInit(): void {
    // Setup search debounce (ER-05)
    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => {
        this.executeSearch(term);
      });

    // Read initial URL query params (ER-05)
    this.route.queryParams.pipe(take(1)).subscribe((params) => {
      const page = params['page'] ? Number(params['page']) : 1;
      const pageSize = params['pageSize'] ? Number(params['pageSize']) : 10;
      const search = params['search'] || undefined;
      const kycStatus = params['kycStatus'] || undefined;
      const status = params['status'] || undefined;

      this.employersService.updateQuery({
        page: !isNaN(page) && page > 0 ? page : 1,
        pageSize: !isNaN(pageSize) && pageSize > 0 ? pageSize : 10,
        search,
        kycStatus,
        status,
      });

      this.employersService.loadMetrics();
    });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  // Scoped retry methods (ER-04)
  retryMetrics(): void {
    this.employersService.loadMetrics();
  }

  retryList(): void {
    this.employersService.loadEmployers();
  }

  retryDetail(): void {
    if (this.selectedEmployerId) {
      this.employersService.loadEmployerDetail(this.selectedEmployerId);
    }
  }

  // Pagination getters & methods (ER-11)
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.pageSize));
  }

  get pageStartIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get pageEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  get pagesList(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
    const delta = 2;
    const range: number[] = [];

    for (let i = Math.max(1, current - delta); i <= Math.min(total, current + delta); i++) {
      range.push(i);
    }
    return range;
  }

  get hasActiveFilters(): boolean {
    return !!(
      (this.searchTerm && this.searchTerm.trim().length > 0) ||
      (this.selectedKycStatus && this.selectedKycStatus.trim().length > 0) ||
      (this.selectedAccountStatus && this.selectedAccountStatus.trim().length > 0)
    );
  }

  onSearchInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchSubject.next(value);
  }

  onSearchEnter(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.executeSearch(value);
  }

  private executeSearch(raw: string): void {
    const trimmed = raw.trim();
    this.searchTerm = trimmed;
    this.currentPage = 1;
    this.syncUrl(1, trimmed, this.selectedKycStatus, this.selectedAccountStatus, this.pageSize);
    this.employersService.updateQuery({
      page: 1,
      search: trimmed.length > 0 ? trimmed : undefined,
    });
  }

  onKycStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value.trim();
    this.selectedKycStatus = value;
    this.currentPage = 1;
    this.syncUrl(1, this.searchTerm, value, this.selectedAccountStatus, this.pageSize);
    this.employersService.updateQuery({
      page: 1,
      kycStatus: value.length > 0 ? value : undefined,
    });
  }

  onAccountStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value.trim();
    this.selectedAccountStatus = value;
    this.currentPage = 1;
    this.syncUrl(1, this.searchTerm, this.selectedKycStatus, value, this.pageSize);
    this.employersService.updateQuery({
      page: 1,
      status: value.length > 0 ? value : undefined,
    });
  }

  onPageSizeChange(event: Event): void {
    const size = Number((event.target as HTMLSelectElement).value);
    if (!isNaN(size) && size > 0) {
      this.pageSize = size;
      this.currentPage = 1;
      this.syncUrl(1, this.searchTerm, this.selectedKycStatus, this.selectedAccountStatus, size);
      this.employersService.updateQuery({
        page: 1,
        pageSize: size,
      });
    }
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedKycStatus = '';
    this.selectedAccountStatus = '';
    this.currentPage = 1;
    this.syncUrl(1, '', '', '', this.pageSize);
    this.employersService.updateQuery({
      page: 1,
      search: undefined,
      kycStatus: undefined,
      status: undefined,
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) {
      return;
    }
    this.currentPage = page;
    this.syncUrl(page, this.searchTerm, this.selectedKycStatus, this.selectedAccountStatus, this.pageSize);
    this.employersService.updateQuery({ page });
  }

  goToPrevPage(): void {
    if (this.currentPage > 1) {
      this.goToPage(this.currentPage - 1);
    }
  }

  goToNextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.goToPage(this.currentPage + 1);
    }
  }

  private syncUrl(
    page: number,
    search?: string,
    kycStatus?: string,
    status?: string,
    pageSize?: number
  ): void {
    const queryParams: Record<string, string | number | null> = {
      page: page > 1 ? page : null,
      pageSize: pageSize && pageSize !== 10 ? pageSize : null,
      search: search && search.length > 0 ? search : null,
      kycStatus: kycStatus && kycStatus.length > 0 ? kycStatus : null,
      status: status && status.length > 0 ? status : null,
    };
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  // Detail Drawer Controls (ER-07)
  openDetail(employer: AdminEmployerItem, event?: MouseEvent): void {
    if (event?.currentTarget instanceof HTMLElement) {
      this.lastFocusedTrigger = event.currentTarget;
    }
    this.selectedEmployerId = employer.id;
    this.isDetailDrawerOpen = true;
    this.employersService.loadEmployerDetail(employer.id);
  }

  closeDetailDrawer(): void {
    this.isDetailDrawerOpen = false;
    this.selectedEmployerId = null;
    this.employersService.clearSelectedEmployer();
    setTimeout(() => {
      this.lastFocusedTrigger?.focus();
    }, 50);
  }

  // Suspend Dialog Controls (ER-08, EC-06)
  openSuspendDialog(employer: AdminEmployerItem, event?: MouseEvent): void {
    if (event?.currentTarget instanceof HTMLElement) {
      this.lastFocusedTrigger = event.currentTarget;
    }
    this.employerToSuspend = employer;
    this.suspendReason = '';
    this.suspendError = null;
    this.isSubmittingSuspend = false;
    this.isSuspendDialogOpen = true;
  }

  closeSuspendDialog(): void {
    if (this.isSubmittingSuspend) return;
    this.isSuspendDialogOpen = false;
    this.employerToSuspend = null;
    this.suspendReason = '';
    this.suspendError = null;
    setTimeout(() => {
      this.lastFocusedTrigger?.focus();
    }, 50);
  }

  confirmSuspend(): void {
    if (!this.employerToSuspend) return;

    const trimmed = this.suspendReason.trim();
    if (!trimmed) {
      this.suspendError = 'Vui lòng nhập lý do đình chỉ tài khoản nhà tuyển dụng.';
      return;
    }

    this.isSubmittingSuspend = true;
    this.suspendError = null;

    const employer = this.employerToSuspend;

    this.employersService
      .updateStatus(
        employer.id,
        {
          action: 'suspend',
          reason: trimmed,
        },
        employer.name
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.isSubmittingSuspend = false;
          this.closeSuspendDialog();
        },
        error: (err) => {
          // Do not close dialog; keep reason intact (ER-08)
          this.isSubmittingSuspend = false;
          this.suspendError =
            err?.error?.message ||
            'Không thể đình chỉ tài khoản. Vui lòng thử lại.';
        },
      });
  }

  // Restore Dialog Controls (ER-09, EC-05)
  openRestoreDialog(employer: AdminEmployerItem, event?: MouseEvent): void {
    if (event?.currentTarget instanceof HTMLElement) {
      this.lastFocusedTrigger = event.currentTarget;
    }
    this.employerToRestore = employer;
    this.isSubmittingRestore = false;
    this.isRestoreDialogOpen = true;
  }

  closeRestoreDialog(): void {
    if (this.isSubmittingRestore) return;
    this.isRestoreDialogOpen = false;
    this.employerToRestore = null;
    setTimeout(() => {
      this.lastFocusedTrigger?.focus();
    }, 50);
  }

  confirmRestore(): void {
    if (!this.employerToRestore) return;

    this.isSubmittingRestore = true;
    const employer = this.employerToRestore;

    this.employersService
      .updateStatus(
        employer.id,
        {
          action: 'restore',
        },
        employer.name
      )
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.isSubmittingRestore = false;
          this.closeRestoreDialog();
        },
        error: () => {
          this.isSubmittingRestore = false;
        },
      });
  }

  // Export action (ER-12)
  exportEmployers(): void {
    const query = this.employersService.currentQuery();
    this.isExporting = true;
    this.employersService
      .getExportUrl({
        format: 'csv',
        search: query.search,
        kycStatus: query.kycStatus,
        status: query.status,
      })
      .pipe(
        take(1),
        finalize(() => {
          this.isExporting = false;
        })
      )
      .subscribe({
        next: (res) => {
          if (res?.downloadUrl) {
            window.open(res.downloadUrl, '_blank', 'noopener');
          }
        },
      });
  }

  // Keyboard shortcut listener (ER-19)
  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    if (this.isSuspendDialogOpen && !this.isSubmittingSuspend) {
      this.closeSuspendDialog();
      return;
    }
    if (this.isRestoreDialogOpen && !this.isSubmittingRestore) {
      this.closeRestoreDialog();
      return;
    }
    if (this.isDetailDrawerOpen) {
      this.closeDetailDrawer();
    }
  }

  // Formatters (ER-14, ER-15)
  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      }).format(d);
    } catch {
      return dateStr;
    }
  }

  getKycBadgeClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'verified':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'pending':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'rejected':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  }

  getKycLabel(status: string): string {
    switch (status?.toLowerCase()) {
      case 'verified':
        return 'Đã xác thực';
      case 'pending':
        return 'Chờ duyệt';
      case 'rejected':
        return 'Bị từ chối';
      default:
        return status || 'Chưa KYC';
    }
  }

  getAccountBadgeClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'suspended':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  }

  getAccountLabel(status: string): string {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'Hoạt động';
      case 'suspended':
        return 'Đã đình chỉ';
      default:
        return status || 'Không xác định';
    }
  }

  isEmployerSuspended(employer: AdminEmployerItem): boolean {
    return employer?.accountStatus?.toLowerCase() === 'suspended';
  }

  trackByEmployer(_: number, item: AdminEmployerItem): string {
    return item.id;
  }
}
