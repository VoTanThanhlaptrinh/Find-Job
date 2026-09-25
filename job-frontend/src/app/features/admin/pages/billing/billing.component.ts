import { Component, OnInit, HostListener, inject, effect, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { take } from 'rxjs';
import { AdminBillingService } from '../../services/admin-billing.service';
import {
  AdminBillingSummary,
  AdminBillingTier,
  AdminBillingTransactionItem,
  AdminUpdateBillingTierPayload,
} from '../../services/admin-api.models';

export type BillingLoadState = 'idle' | 'loading' | 'success' | 'empty' | 'error';

export interface TierEditFormValue {
  priceMonthly: number;
  features: string[];
}

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './billing.component.html',
})
export class BillingComponent implements OnInit {
  private readonly billingService = inject(AdminBillingService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Data signals mirroring service state
  summary: AdminBillingSummary | null = null;
  tiers: AdminBillingTier[] = [];
  transactions: AdminBillingTransactionItem[] = [];
  totalTransactions = 0;

  // Scoped loading and error states
  isLoadingSummary = false;
  isLoadingTiers = false;
  isLoadingTransactions = false;
  updatingTierId: string | null = null;

  summaryError: string | null = null;
  tiersError: string | null = null;
  transactionsError: string | null = null;

  // Query & pagination state
  selectedTransactionStatus = '';
  currentPage = 1;
  readonly pageSize = 20;

  // Edit Tier Drawer state
  isDrawerOpen = false;
  editingTier: AdminBillingTier | null = null;
  formPriceMonthly = 0;
  formFeatures: string[] = [];
  formFeaturesRaw = '';
  formNewFeature = '';
  isRawFeatureMode = false;
  drawerError: string | null = null;
  isSavingTier = false;
  showUnsavedDialog = false;
  private lastFocusedTrigger: HTMLElement | null = null;

  constructor() {
    effect(() => {
      this.summary = this.billingService.summary();
      this.tiers = this.billingService.tiers();
      this.transactions = this.billingService.transactions();
      this.totalTransactions = this.billingService.totalTransactions();

      this.isLoadingSummary = this.billingService.isLoadingSummary();
      this.isLoadingTiers = this.billingService.isLoadingTiers();
      this.isLoadingTransactions = this.billingService.isLoadingTransactions();
      this.updatingTierId = this.billingService.updatingTierId();

      this.summaryError = this.billingService.summaryError();
      this.tiersError = this.billingService.tiersError();
      this.transactionsError = this.billingService.transactionsError();

      const query = this.billingService.transactionsQuery();
      this.selectedTransactionStatus = query.status ?? '';
      this.currentPage = query.page ?? 1;
    });
  }

  ngOnInit(): void {
    // Initial fetch for summary & tiers
    this.billingService.loadSummary();
    this.billingService.loadTiers();

    // Read initial URL query params to sync status filter & pagination (BL-14)
    this.route.queryParams.pipe(take(1)).subscribe((params) => {
      const pageParam = Number(params['page']);
      const statusParam = params['status'];
      const page = !isNaN(pageParam) && pageParam > 0 ? pageParam : 1;
      const status = typeof statusParam === 'string' && statusParam.trim().length > 0 ? statusParam.trim() : undefined;

      this.billingService.updateTransactionsQuery({
        page,
        status,
        pageSize: this.pageSize,
      });
    });
  }

  // Reload all widgets
  reloadAll(): void {
    this.billingService.loadSummary();
    this.billingService.loadTiers();
    this.billingService.loadTransactions();
  }

  // Scoped retry methods (BL-10)
  retrySummary(): void {
    this.billingService.loadSummary();
  }

  retryTiers(): void {
    this.billingService.loadTiers();
  }

  retryTransactions(): void {
    this.billingService.loadTransactions();
  }

  // Pagination getters
  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalTransactions / this.pageSize));
  }

  get pageStartIndex(): number {
    if (this.totalTransactions === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get pageEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalTransactions);
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

  // Query & URL sync handlers (BL-14)
  onTransactionsStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value.trim();
    this.selectedTransactionStatus = value;
    this.currentPage = 1;

    this.syncUrl(1, value);
    this.billingService.updateTransactionsQuery({
      page: 1,
      status: value.length > 0 ? value : undefined,
    });
  }

  clearStatusFilter(): void {
    this.selectedTransactionStatus = '';
    this.currentPage = 1;
    this.syncUrl(1, '');
    this.billingService.updateTransactionsQuery({
      page: 1,
      status: undefined,
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) {
      return;
    }
    this.currentPage = page;
    this.syncUrl(page, this.selectedTransactionStatus);
    this.billingService.updateTransactionsQuery({ page });
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

  private syncUrl(page: number, status?: string): void {
    const queryParams: Record<string, string | number | null> = {
      page: page > 1 ? page : null,
      status: status && status.length > 0 ? status : null,
    };
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  // Drawer & Edit Tier (BL-04, BL-05, BL-06, BL-07, BL-15, BL-16)
  openEditDrawer(tier: AdminBillingTier, event?: MouseEvent): void {
    if (event?.currentTarget instanceof HTMLElement) {
      this.lastFocusedTrigger = event.currentTarget;
    }

    this.editingTier = tier;
    this.formPriceMonthly = tier.priceMonthly;
    this.formFeatures = [...tier.features];
    this.formFeaturesRaw = tier.features.join('\n');
    this.formNewFeature = '';
    this.drawerError = null;
    this.isRawFeatureMode = false;
    this.showUnsavedDialog = false;
    this.isDrawerOpen = true;
  }

  get isFormDirty(): boolean {
    if (!this.editingTier) return false;
    if (this.formPriceMonthly !== this.editingTier.priceMonthly) return true;

    // Compare features array
    if (this.formFeatures.length !== this.editingTier.features.length) return true;
    for (let i = 0; i < this.formFeatures.length; i++) {
      if (this.formFeatures[i] !== this.editingTier.features[i]) return true;
    }
    return false;
  }

  get isFormValid(): boolean {
    return (
      !isNaN(this.formPriceMonthly) &&
      this.formPriceMonthly > 0 &&
      this.formFeatures.length > 0
    );
  }

  attemptCloseDrawer(): void {
    if (this.isFormDirty && !this.isSavingTier) {
      this.showUnsavedDialog = true;
      return;
    }
    this.closeDrawer();
  }

  confirmDiscardChanges(): void {
    this.showUnsavedDialog = false;
    this.closeDrawer();
  }

  cancelDiscardChanges(): void {
    this.showUnsavedDialog = false;
  }

  closeDrawer(): void {
    this.isDrawerOpen = false;
    this.editingTier = null;
    this.drawerError = null;
    this.showUnsavedDialog = false;
    this.isSavingTier = false;

    // Return focus to trigger button (BL-15)
    setTimeout(() => {
      this.lastFocusedTrigger?.focus();
    }, 50);
  }

  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    if (this.showUnsavedDialog) {
      this.showUnsavedDialog = false;
      return;
    }
    if (this.isDrawerOpen && !this.isSavingTier) {
      this.attemptCloseDrawer();
    }
  }

  // Feature item management in Drawer
  addFeature(): void {
    const trimmed = this.formNewFeature.trim();
    if (!trimmed) return;

    if (!this.formFeatures.includes(trimmed)) {
      this.formFeatures.push(trimmed);
      this.formFeaturesRaw = this.formFeatures.join('\n');
    }
    this.formNewFeature = '';
  }

  onFeatureInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addFeature();
    }
  }

  removeFeature(index: number): void {
    if (index >= 0 && index < this.formFeatures.length) {
      this.formFeatures.splice(index, 1);
      this.formFeaturesRaw = this.formFeatures.join('\n');
    }
  }

  toggleRawFeatureMode(): void {
    if (this.isRawFeatureMode) {
      // Exiting raw mode: parse textarea
      this.parseRawFeatures();
    } else {
      // Entering raw mode: sync textarea
      this.formFeaturesRaw = this.formFeatures.join('\n');
    }
    this.isRawFeatureMode = !this.isRawFeatureMode;
  }

  parseRawFeatures(): void {
    this.formFeatures = this.formFeaturesRaw
      .split('\n')
      .map((item) => item.trim())
      .filter((item) => item.length > 0);
  }

  // Submit Tier Update (BL-05, BL-07, BL-16)
  submitSaveTier(): void {
    if (this.isRawFeatureMode) {
      this.parseRawFeatures();
    }

    if (!this.editingTier || !this.isFormValid || this.isSavingTier) {
      return;
    }

    this.isSavingTier = true;
    this.drawerError = null;

    const payload: AdminUpdateBillingTierPayload = {
      priceMonthly: Number(this.formPriceMonthly),
      features: [...this.formFeatures],
    };

    const tierToUpdate = this.editingTier;

    this.billingService
      .updateTier(tierToUpdate.id, payload, tierToUpdate.name)
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.isSavingTier = false;
          this.closeDrawer();
        },
        error: (err) => {
          // Do NOT close drawer; keep user input intact (BL-07)
          this.isSavingTier = false;
          this.drawerError =
            err?.error?.message ||
            'Không thể lưu thay đổi gói cước. Vui lòng kiểm tra lại thông tin và thử lại.';
        },
      });
  }

  // Formatters (BL-03, BL-12, BL-13)
  formatCurrency(amount: number | null | undefined, currency: string = 'VND'): string {
    if (amount === null || amount === undefined || isNaN(amount)) {
      return '0 ' + currency;
    }
    const curr = (currency || 'VND').toUpperCase();
    try {
      return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: curr,
        maximumFractionDigits: curr === 'VND' ? 0 : 2,
      }).format(amount);
    } catch {
      return `${amount.toLocaleString('vi-VN')} ${curr}`;
    }
  }

  formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      return new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false,
      }).format(d);
    } catch {
      return dateStr;
    }
  }

  getStatusBadgeClass(status: string): string {
    switch (status?.toLowerCase()) {
      case 'paid':
      case 'success':
      case 'completed':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-800';
      case 'pending':
      case 'processing':
        return 'bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/40 dark:text-amber-300 dark:border-amber-800';
      case 'failed':
      case 'cancelled':
      case 'error':
        return 'bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-950/40 dark:text-rose-300 dark:border-rose-800';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';
    }
  }

  getStatusLabel(status: string): string {
    switch (status?.toLowerCase()) {
      case 'paid':
      case 'success':
      case 'completed':
        return 'Đã thanh toán';
      case 'pending':
      case 'processing':
        return 'Đang chờ xử lý';
      case 'failed':
      case 'cancelled':
      case 'error':
        return 'Thất bại';
      default:
        return status || 'Không xác định';
    }
  }

  trackByTier(_: number, item: AdminBillingTier): string {
    return item.id;
  }

  trackByTransaction(_: number, item: AdminBillingTransactionItem): string {
    return item.id;
  }
}
