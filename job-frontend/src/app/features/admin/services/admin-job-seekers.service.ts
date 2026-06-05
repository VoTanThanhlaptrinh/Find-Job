import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, map, Observable, take, tap, catchError, throwError } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminCreateJobSeekerData,
  AdminCreateJobSeekerPayload,
  AdminJobSeekerItem,
  AdminJobSeekerListQuery,
  AdminJobSeekersMetrics,
  AdminListPayload,
  AdminRegionDistribution,
} from './admin-api.models';
import { buildHttpParams } from './admin-http.utils';

@Injectable({
  providedIn: 'root',
})
export class AdminJobSeekersService {
  private readonly url: string;

  // Internal state signals
  private readonly _metrics = signal<AdminJobSeekersMetrics | null>(null);
  private readonly _jobSeekers = signal<AdminJobSeekerItem[]>([]);
  private readonly _totalItems = signal(0);
  private readonly _regionDistribution = signal<AdminRegionDistribution | null>(null);
  
  // Query state signal
  private readonly _currentQuery = signal<AdminJobSeekerListQuery>({
    page: 1,
    pageSize: 10
  });

  private readonly _isLoadingMetrics = signal(false);
  private readonly _isLoadingList = signal(false);
  private readonly _isCreating = signal(false);
  private readonly _isLoadingRegions = signal(false);

  // Public computed signals
  readonly metrics = computed(() => this._metrics());
  readonly jobSeekers = computed(() => this._jobSeekers());
  readonly totalItems = computed(() => this._totalItems());
  readonly regionDistribution = computed(() => this._regionDistribution());
  readonly currentQuery = computed(() => this._currentQuery());

  readonly isLoadingMetrics = computed(() => this._isLoadingMetrics());
  readonly isLoadingList = computed(() => this._isLoadingList());
  readonly isCreating = computed(() => this._isCreating());
  readonly isLoadingRegions = computed(() => this._isLoadingRegions());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Load job seeker metrics
   */
  loadMetrics(): void {
    this._isLoadingMetrics.set(true);
    this.http
      .get<ApiResponse<AdminJobSeekersMetrics>>(`${this.url}/admin/job-seekers/metrics`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingMetrics.set(false))
      )
      .subscribe({
        next: (res) => this._metrics.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải chỉ số người tìm việc')
      });
  }

  /**
   * Update query and reload list
   */
  updateQuery(patch: Partial<AdminJobSeekerListQuery>): void {
    this._currentQuery.update(q => ({ ...q, ...patch }));
    this.loadJobSeekers();
  }

  /**
   * Load job seekers list based on current query signal
   */
  loadJobSeekers(): void {
    this._isLoadingList.set(true);
    const params = buildHttpParams(this._currentQuery());
    this.http
      .get<ApiResponse<AdminListPayload<AdminJobSeekerItem>>>(`${this.url}/admin/job-seekers`, {
        params,
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingList.set(false))
      )
      .subscribe({
        next: (res) => {
          this._jobSeekers.set(res.data.items);
          this._totalItems.set(res.data.pagination.totalItems);
        },
        error: (err) => {
          this._jobSeekers.set([]);
          this._totalItems.set(0);
          this.handleError(err, 'Không thể tải danh sách người tìm việc');
        }
      });
  }

  /**
   * Create a new job seeker profile
   */
  createJobSeeker(payload: AdminCreateJobSeekerPayload): Observable<AdminCreateJobSeekerData> {
    this._isCreating.set(true);
    return this.http
      .post<ApiResponse<AdminCreateJobSeekerData>>(`${this.url}/admin/job-seekers`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => res.data),
        tap(() => {
          this.notify.success('Tạo mới người tìm việc thành công');
          this.loadMetrics();
          this.loadJobSeekers();
        }),
        catchError(err => {
          this.handleError(err, 'Lỗi khi tạo người tìm việc');
          return throwError(() => err);
        }),
        finalize(() => this._isCreating.set(false))
      );
  }

  /**
   * Load region distribution data
   */
  loadRegionDistribution(): void {
    this._isLoadingRegions.set(true);
    this.http
      .get<ApiResponse<AdminRegionDistribution>>(`${this.url}/admin/job-seekers/region-distribution`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => this._isLoadingRegions.set(false))
      )
      .subscribe({
        next: (res) => this._regionDistribution.set(res.data),
        error: (err) => this.handleError(err, 'Không thể tải phân bổ vùng miền')
      });
  }

  private handleError(err: any, defaultMsg: string): void {
    const msg = err?.error?.message || defaultMsg;
    this.notify.error(msg);
  }
}
