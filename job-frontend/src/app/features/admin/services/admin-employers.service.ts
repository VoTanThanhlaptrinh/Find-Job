import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, map, Observable, take, tap, catchError, throwError } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminEmployerDetail,
  AdminEmployerItem,
  AdminEmployerListQuery,
  AdminEmployersExportData,
  AdminEmployersExportQuery,
  AdminEmployersMetrics,
  AdminListPayload,
  AdminUpdateEmployerStatusData,
  AdminUpdateEmployerStatusPayload,
} from './admin-api.models';
import { buildHttpParams } from './admin-http.utils';

@Injectable({
  providedIn: 'root',
})
export class AdminEmployersService {
  private readonly url: string;

  // Internal state signals
  private readonly _metrics = signal<AdminEmployersMetrics | null>(null);
  private readonly _employers = signal<AdminEmployerItem[]>([]);
  private readonly _totalItems = signal(0);
  private readonly _selectedEmployer = signal<AdminEmployerDetail | null>(null);
  
  // Query state signal
  private readonly _currentQuery = signal<AdminEmployerListQuery>({
    page: 1,
    pageSize: 10
  });

  private readonly _isLoadingMetrics = signal(false);
  private readonly _isLoadingList = signal(false);
  private readonly _isLoadingDetail = signal(false);

  // Public computed signals
  readonly metrics = computed(() => this._metrics());
  readonly employers = computed(() => this._employers());
  readonly totalItems = computed(() => this._totalItems());
  readonly selectedEmployer = computed(() => this._selectedEmployer());
  readonly currentQuery = computed(() => this._currentQuery());

  readonly isLoadingMetrics = computed(() => this._isLoadingMetrics());
  readonly isLoadingList = computed(() => this._isLoadingList());
  readonly isLoadingDetail = computed(() => this._isLoadingDetail());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Load employer metrics with auto-notification on error
   */
  loadMetrics(): void {
    this._isLoadingMetrics.set(true);
    this.http
      .get<ApiResponse<AdminEmployersMetrics>>(`${this.url}/admin/employers/metrics`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingMetrics.set(false))
      )
      .subscribe({
        next: (res) => this._metrics.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải chỉ số nhà tuyển dụng')
      });
  }

  /**
   * Update query and reload list
   */
  updateQuery(patch: Partial<AdminEmployerListQuery>): void {
    this._currentQuery.update(q => ({ ...q, ...patch }));
    this.loadEmployers();
  }

  /**
   * Load employers list based on current query signal
   */
  loadEmployers(): void {
    this._isLoadingList.set(true);
    const params = buildHttpParams(this._currentQuery());
    this.http
      .get<ApiResponse<AdminListPayload<AdminEmployerItem>>>(`${this.url}/admin/employers`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingList.set(false))
      )
      .subscribe({
        next: (res) => {
          this._employers.set(res.data.items);
          this._totalItems.set(res.data.pagination.totalItems);
        },
        error: (err) => {
          this._employers.set([]);
          this._totalItems.set(0);
          this.handleError(err, 'Không thể tải danh sách nhà tuyển dụng');
        }
      });
  }

  /**
   * Load a single employer's details
   */
  loadEmployerDetail(id: string): void {
    this._isLoadingDetail.set(true);
    this.http
      .get<ApiResponse<AdminEmployerDetail>>(`${this.url}/admin/employers/${id}`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingDetail.set(false))
      )
      .subscribe({
        next: (res) => this._selectedEmployer.set(res.data),
        error: (err) => {
          this._selectedEmployer.set(null);
          this.handleError(err, 'Không thể tải thông tin chi tiết nhà tuyển dụng');
        }
      });
  }

  /**
   * Update employer account status
   */
  updateStatus(id: string, payload: AdminUpdateEmployerStatusPayload): Observable<AdminUpdateEmployerStatusData> {
    return this.http
      .patch<ApiResponse<AdminUpdateEmployerStatusData>>(`${this.url}/admin/employers/${id}/status`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        tap((updatedData) => {
          this.notify.success(`Cập nhật trạng thái nhà tuyển dụng thành công: ${updatedData.accountStatus}`);
          this._employers.update(items => items.map(item => 
            item.id === id ? { ...item, accountStatus: updatedData.accountStatus } : item
          ));
          if (this._selectedEmployer()?.id === id) {
            this._selectedEmployer.update(curr => curr ? { ...curr, accountStatus: updatedData.accountStatus } : null);
          }
        }),
        catchError(err => {
          this.handleError(err, 'Cập nhật trạng thái thất bại');
          return throwError(() => err);
        })
      );
  }

  /**
   * Get export URL
   */
  getExportUrl(query: AdminEmployersExportQuery): Observable<AdminEmployersExportData> {
    const params = buildHttpParams(query);
    return this.http
      .get<ApiResponse<AdminEmployersExportData>>(`${this.url}/admin/employers/export`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        catchError(err => {
          this.handleError(err, 'Lỗi khi yêu cầu xuất dữ liệu');
          return throwError(() => err);
        })
      );
  }

  private handleError(err: any, defaultMsg: string): void {
    const msg = err?.error?.message || defaultMsg;
    this.notify.error(msg);
  }
}
