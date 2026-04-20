import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, take, catchError, throwError } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminDashboardSummary,
  AdminJobDistribution,
  AdminListPayload,
  AdminPendingJobItem,
  AdminPendingJobsQuery,
  AdminRevenueTrend,
} from './admin-api.models';
import { buildHttpParams } from './admin-http.utils';

@Injectable({
  providedIn: 'root',
})
export class AdminDashboardService {
  private readonly url: string;

  // Internal state signals
  private readonly _summary = signal<AdminDashboardSummary | null>(null);
  private readonly _revenueTrend = signal<AdminRevenueTrend | null>(null);
  private readonly _jobDistribution = signal<AdminJobDistribution | null>(null);
  private readonly _pendingJobs = signal<AdminPendingJobItem[]>([]);
  private readonly _pendingJobsTotal = signal(0);
  
  // Pending jobs query signal
  private readonly _pendingJobsQuery = signal<AdminPendingJobsQuery>({
    page: 1,
    pageSize: 10
  });

  private readonly _isLoadingSummary = signal(false);
  private readonly _isLoadingRevenue = signal(false);
  private readonly _isLoadingDistribution = signal(false);
  private readonly _isLoadingPendingJobs = signal(false);

  // Public computed signals
  readonly summary = computed(() => this._summary());
  readonly revenueTrend = computed(() => this._revenueTrend());
  readonly jobDistribution = computed(() => this._jobDistribution());
  readonly pendingJobs = computed(() => this._pendingJobs());
  readonly pendingJobsTotal = computed(() => this._pendingJobsTotal());
  readonly pendingJobsQuery = computed(() => this._pendingJobsQuery());
  
  readonly isLoadingSummary = computed(() => this._isLoadingSummary());
  readonly isLoadingRevenue = computed(() => this._isLoadingRevenue());
  readonly isLoadingDistribution = computed(() => this._isLoadingDistribution());
  readonly isLoadingPendingJobs = computed(() => this._isLoadingPendingJobs());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Load dashboard summary metrics
   */
  loadSummary(): void {
    this._isLoadingSummary.set(true);
    this.http
      .get<ApiResponse<AdminDashboardSummary>>(`${this.url}/admin/dashboard/summary`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingSummary.set(false))
      )
      .subscribe({
        next: (res) => this._summary.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải thông tin tóm tắt Dashboard')
      });
  }

  /**
   * Load revenue trend chart data
   */
  loadRevenueTrend(range: string = '30d'): void {
    this._isLoadingRevenue.set(true);
    const params = buildHttpParams({ range });
    this.http
      .get<ApiResponse<AdminRevenueTrend>>(`${this.url}/admin/dashboard/revenue-trend`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingRevenue.set(false))
      )
      .subscribe({
        next: (res) => this._revenueTrend.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải xu hướng doanh thu')
      });
  }

  /**
   * Load job distribution metrics
   */
  loadJobDistribution(): void {
    this._isLoadingDistribution.set(true);
    this.http
      .get<ApiResponse<AdminJobDistribution>>(`${this.url}/admin/dashboard/job-distribution`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingDistribution.set(false))
      )
      .subscribe({
        next: (res) => this._jobDistribution.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải phân bổ công việc')
      });
  }

  /**
   * Update pending jobs query and reload
   */
  updatePendingJobsQuery(patch: Partial<AdminPendingJobsQuery>): void {
    this._pendingJobsQuery.update(q => ({ ...q, ...patch }));
    this.loadPendingJobs();
  }

  /**
   * Load pending jobs based on current query signal
   */
  loadPendingJobs(): void {
    this._isLoadingPendingJobs.set(true);
    const params = buildHttpParams(this._pendingJobsQuery());

    this.http
      .get<ApiResponse<AdminListPayload<AdminPendingJobItem>>>(`${this.url}/admin/jobs/pending`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingPendingJobs.set(false))
      )
      .subscribe({
        next: (res) => {
          this._pendingJobs.set(res.data.items);
          this._pendingJobsTotal.set(res.data.pagination.totalItems);
        },
        error: (err) => {
          this._pendingJobs.set([]);
          this._pendingJobsTotal.set(0);
          this.handleError(err, 'Không thể tải danh sách công việc chờ duyệt');
        }
      });
  }

  /**
   * Helper to refresh all dashboard data
   */
  refreshAll(): void {
    this.loadSummary();
    this.loadRevenueTrend();
    this.loadJobDistribution();
    this.loadPendingJobs();
  }

  private handleError(err: any, defaultMsg: string): void {
    const msg = err?.error?.message || defaultMsg;
    this.notify.error(msg);
  }
}
