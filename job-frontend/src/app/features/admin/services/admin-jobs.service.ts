import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, map, Observable, take, tap, catchError, throwError } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminBulkJobActionData,
  AdminBulkJobActionPayload,
  AdminCreateJobData,
  AdminCreateJobPayload,
  AdminJobItem,
  AdminJobsListQuery,
  AdminJobsMetrics,
  AdminListPayload,
  AdminUpdateJobStatusData,
  AdminUpdateJobStatusPayload,
} from './admin-api.models';
import { buildHttpParams } from './admin-http.utils';

@Injectable({
  providedIn: 'root',
})
export class AdminJobsService {
  private readonly url: string;

  // Internal state signals
  private readonly _metrics = signal<AdminJobsMetrics | null>(null);
  private readonly _jobs = signal<AdminJobItem[]>([]);
  private readonly _totalItems = signal(0);
  
  // Query state signal
  private readonly _currentQuery = signal<AdminJobsListQuery>({
    page: 1,
    pageSize: 12
  });

  private readonly _isLoadingMetrics = signal(false);
  private readonly _isLoadingList = signal(false);
  private readonly _isCreating = signal(false);
  private readonly _isUpdatingStatus = signal<string | null>(null); // Job ID being updated

  // Public computed signals
  readonly metrics = computed(() => this._metrics());
  readonly jobs = computed(() => this._jobs());
  readonly totalItems = computed(() => this._totalItems());
  readonly currentQuery = computed(() => this._currentQuery());

  readonly isLoadingMetrics = computed(() => this._isLoadingMetrics());
  readonly isLoadingList = computed(() => this._isLoadingList());
  readonly isCreating = computed(() => this._isCreating());
  readonly updatingStatusJobId = computed(() => this._isUpdatingStatus());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Load job postings metrics
   */
  loadMetrics(): void {
    this._isLoadingMetrics.set(true);
    this.http
      .get<ApiResponse<AdminJobsMetrics>>(`${this.url}/admin/jobs/metrics`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingMetrics.set(false))
      )
      .subscribe({
        next: (res) => this._metrics.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải chỉ số tin tuyển dụng')
      });
  }

  /**
   * Update query and reload list
   */
  updateQuery(patch: Partial<AdminJobsListQuery>): void {
    this._currentQuery.update(q => ({ ...q, ...patch }));
    this.loadJobs();
  }

  /**
   * Load jobs list based on current query signal
   */
  loadJobs(): void {
    this._isLoadingList.set(true);
    const params = buildHttpParams(this._currentQuery());
    this.http
      .get<ApiResponse<AdminListPayload<AdminJobItem>>>(`${this.url}/admin/jobs`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingList.set(false))
      )
      .subscribe({
        next: (res) => {
          this._jobs.set(res.data.items);
          this._totalItems.set(res.data.pagination.totalItems);
        },
        error: (err) => {
          this._jobs.set([]);
          this._totalItems.set(0);
          this.handleError(err, 'Không thể tải danh sách tin tuyển dụng');
        }
      });
  }

  /**
   * Create a new job posting from admin panel
   */
  createJob(payload: AdminCreateJobPayload): Observable<AdminCreateJobData> {
    this._isCreating.set(true);
    return this.http
      .post<ApiResponse<AdminCreateJobData>>(`${this.url}/admin/jobs`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        tap(() => {
          this.notify.success('Tạo tin tuyển dụng mới thành công');
          this.loadMetrics();
          this.loadJobs();
        }),
        catchError(err => {
          this.handleError(err, 'Lỗi khi tạo tin tuyển dụng');
          return throwError(() => err);
        }),
        finalize(() => this._isCreating.set(false))
      );
  }

  /**
   * Update a job's status
   */
  updateJobStatus(id: string, payload: AdminUpdateJobStatusPayload): Observable<AdminUpdateJobStatusData> {
    this._isUpdatingStatus.set(id);
    return this.http
      .patch<ApiResponse<AdminUpdateJobStatusData>>(`${this.url}/admin/jobs/${id}/status`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        tap((updated) => {
          this.notify.success(`Cập nhật trạng thái tin tuyển dụng #${id} thành công`);
          this._jobs.update(items => items.map(item => 
            item.id === id ? { ...item, status: updated.status } : item
          ));
        }),
        catchError(err => {
          this.handleError(err, 'Cập nhật trạng thái thất bại');
          return throwError(() => err);
        }),
        finalize(() => this._isUpdatingStatus.set(null))
      );
  }

  /**
   * Perform bulk action on multiple jobs
   */
  bulkAction(payload: AdminBulkJobActionPayload): Observable<AdminBulkJobActionData> {
    return this.http
      .post<ApiResponse<AdminBulkJobActionData>>(`${this.url}/admin/jobs/bulk-action`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        tap((res) => {
          this.notify.success(`Đã xử lý hàng loạt ${res.processed} tin tuyển dụng thành công`);
          this.loadMetrics();
          this.loadJobs();
        }),
        catchError(err => {
          this.handleError(err, 'Lỗi khi thực hiện thao tác hàng loạt');
          return throwError(() => err);
        })
      );
  }

  private handleError(err: any, defaultMsg: string): void {
    const msg = err?.error?.message || defaultMsg;
    this.notify.error(msg);
  }
}
