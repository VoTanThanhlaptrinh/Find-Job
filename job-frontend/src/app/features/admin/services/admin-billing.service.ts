import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, map, Observable, take, tap, catchError, throwError } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminBillingSummary,
  AdminBillingTransactionItem,
  AdminBillingTransactionsQuery,
  AdminBillingTier,
  AdminBillingTiersData,
  AdminListPayload,
  AdminUpdateBillingTierData,
  AdminUpdateBillingTierPayload,
} from './admin-api.models';
import { buildHttpParams } from './admin-http.utils';

@Injectable({
  providedIn: 'root',
})
export class AdminBillingService {
  private readonly url: string;

  // Internal state signals
  private readonly _tiers = signal<AdminBillingTier[]>([]);
  private readonly _transactions = signal<AdminBillingTransactionItem[]>([]);
  private readonly _totalTransactions = signal(0);
  private readonly _summary = signal<AdminBillingSummary | null>(null);
  
  // Transactions query signal
  private readonly _transactionsQuery = signal<AdminBillingTransactionsQuery>({
    page: 1,
    pageSize: 20
  });

  private readonly _isLoadingTiers = signal(false);
  private readonly _isLoadingTransactions = signal(false);
  private readonly _isLoadingSummary = signal(false);
  private readonly _isUpdatingTier = signal<string | null>(null);

  private readonly _summaryError = signal<string | null>(null);
  private readonly _tiersError = signal<string | null>(null);
  private readonly _transactionsError = signal<string | null>(null);

  // Public computed signals
  readonly tiers = computed(() => this._tiers());
  readonly transactions = computed(() => this._transactions());
  readonly totalTransactions = computed(() => this._totalTransactions());
  readonly summary = computed(() => this._summary());
  readonly transactionsQuery = computed(() => this._transactionsQuery());

  readonly isLoadingTiers = computed(() => this._isLoadingTiers());
  readonly isLoadingTransactions = computed(() => this._isLoadingTransactions());
  readonly isLoadingSummary = computed(() => this._isLoadingSummary());
  readonly updatingTierId = computed(() => this._isUpdatingTier());

  readonly summaryError = computed(() => this._summaryError());
  readonly tiersError = computed(() => this._tiersError());
  readonly transactionsError = computed(() => this._transactionsError());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Load subscription tiers
   */
  loadTiers(): void {
    this._isLoadingTiers.set(true);
    this._tiersError.set(null);
    this.http
      .get<ApiResponse<AdminBillingTiersData | AdminBillingTier[]>>(`${this.url}/admin/billing/tiers`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingTiers.set(false))
      )
      .subscribe({
        next: (res) => {
          const payload = res.data;
          const tiers = Array.isArray(payload) ? payload : payload?.tiers || [];
          this._tiers.set(tiers);
          this._tiersError.set(null);
        },
        error: (err) => {
          const msg = err?.error?.message || 'Không thể tải danh sách gói cước';
          this._tiersError.set(msg);
          this.handleError(err, 'Không thể tải danh sách gói cước');
        }
      });
  }

  /**
   * Update a specific subscription tier
   */
  updateTier(id: string, payload: AdminUpdateBillingTierPayload, tierName?: string): Observable<AdminUpdateBillingTierData> {
    this._isUpdatingTier.set(id);
    return this.http
      .patch<ApiResponse<AdminUpdateBillingTierData | boolean>>(`${this.url}/admin/billing/tiers/${id}`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => {
          if (typeof res.data === 'boolean') {
            return {
              id,
              updated: res.data,
            } as AdminUpdateBillingTierData;
          }

          return res.data;
        }),
        tap(() => {
          const successMsg = tierName
            ? `Cập nhật gói cước "${tierName}" thành công`
            : 'Cập nhật gói cước thành công';
          this.notify.success(successMsg);
          this.loadTiers();
          this.loadSummary();
        }),
        catchError(err => {
          this.handleError(err, 'Lỗi khi cập nhật gói cước');
          return throwError(() => err);
        }),
        finalize(() => this._isUpdatingTier.set(null))
      );
  }

  /**
   * Update transaction query and reload list
   */
  updateTransactionsQuery(patch: Partial<AdminBillingTransactionsQuery>): void {
    this._transactionsQuery.update(q => ({ ...q, ...patch }));
    this.loadTransactions();
  }

  /**
   * Load billing transactions based on current query signal
   */
  loadTransactions(): void {
    this._isLoadingTransactions.set(true);
    this._transactionsError.set(null);
    const params = buildHttpParams(this._transactionsQuery());
    this.http
      .get<ApiResponse<AdminListPayload<AdminBillingTransactionItem>>>(`${this.url}/admin/billing/transactions`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingTransactions.set(false))
      )
      .subscribe({
        next: (res) => {
          this._transactions.set(res.data?.items || []);
          this._totalTransactions.set(res.data?.pagination?.totalItems || 0);
          this._transactionsError.set(null);
        },
        error: (err) => {
          const msg = err?.error?.message || 'Không thể tải lịch sử giao dịch';
          this._transactionsError.set(msg);
          this.handleError(err, 'Không thể tải lịch sử giao dịch');
        }
      });
  }

  /**
   * Load billing/revenue summary metrics
   */
  loadSummary(): void {
    this._isLoadingSummary.set(true);
    this._summaryError.set(null);
    this.http
      .get<ApiResponse<AdminBillingSummary>>(`${this.url}/admin/billing/summary`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingSummary.set(false))
      )
      .subscribe({
        next: (res) => {
          this._summary.set(res.data);
          this._summaryError.set(null);
        },
        error: (err) => {
          const msg = err?.error?.message || 'Không thể tải tóm tắt doanh thu';
          this._summaryError.set(msg);
          this.handleError(err, 'Không thể tải tóm tắt doanh thu');
        }
      });
  }

  private handleError(err: any, defaultMsg: string): void {
    const msg = err?.error?.message || defaultMsg;
    this.notify.error(msg);
  }
}
