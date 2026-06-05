import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, take, tap } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import {
  AddressCountViewModel,
  JobAddressCountApiResponse,
  JobCountApiResponse,
  JobFilterPayload,
  JobListApiResponse,
  PagedPayload,
} from '../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../shared/models/jobs/job-card.model';
import { ApiResponse } from '../../../shared/models/api-response.model';

const DEFAULT_JOB_FILTER: JobFilterPayload = {
  pageIndex: 0,
  pageSize: 5,
  address: [],
  times: [],
  title: '',
};

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private static readonly MIN_LOADING_MS = 1000;

  private url: string;
  private jobData = signal<JobCardModel[]>([]);
  private addressData = signal<AddressCountViewModel[]>([]);
  private totalJobData = signal<number | null>(null);
  private loadingJobs = signal(false);
  private filterPayload = signal<JobFilterPayload>({ ...DEFAULT_JOB_FILTER });
  private activeJobRequests = 0;
  private loadingStartAt = 0;
  private hideLoadingTimeout: ReturnType<typeof setTimeout> | null = null;

  jobs = computed(() => this.jobData());
  addressCount = computed(() => this.addressData());
  totalJobs = computed(() => this.totalJobData());
  isLoadingJobs = computed(() => this.loadingJobs());
  jobFilter = computed(() => this.filterPayload());

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  getFilterSnapshot(): JobFilterPayload {
    return this.cloneFilter(this.filterPayload());
  }

  setFilterPayload(filter: JobFilterPayload): void {
    this.filterPayload.set(this.cloneFilter(filter));
  }

  updateFilterPayload(partial: Partial<JobFilterPayload>): void {
    this.filterPayload.update((current) => this.cloneFilter({ ...current, ...partial }));
  }

  resetFilterPayload(overrides: Partial<JobFilterPayload> = {}): void {
    this.filterPayload.set(this.cloneFilter({ ...DEFAULT_JOB_FILTER, ...overrides }));
  }

  private cloneFilter(filter: JobFilterPayload): JobFilterPayload {
    return {
      pageIndex: Number.isFinite(filter.pageIndex) ? filter.pageIndex : DEFAULT_JOB_FILTER.pageIndex,
      pageSize: Number.isFinite(filter.pageSize) ? filter.pageSize : DEFAULT_JOB_FILTER.pageSize,
      address: Array.isArray(filter.address) ? [...filter.address] : [],
      times: Array.isArray(filter.times) ? [...filter.times] : [],
      title: (filter.title ?? '').trim(),
    };
  }

  private startJobsLoading(): void {
    if (this.hideLoadingTimeout) {
      clearTimeout(this.hideLoadingTimeout);
      this.hideLoadingTimeout = null;
    }

    this.activeJobRequests += 1;
    if (this.activeJobRequests === 1) {
      this.loadingStartAt = Date.now();
      this.loadingJobs.set(true);
    }
  }

  private finishJobsLoading(): void {
    if (this.activeJobRequests <= 0) {
      return;
    }

    this.activeJobRequests -= 1;
    if (this.activeJobRequests > 0) {
      return;
    }

    const elapsed = Date.now() - this.loadingStartAt;
    const remaining = Math.max(0, CategoryService.MIN_LOADING_MS - elapsed);

    if (remaining === 0) {
      this.loadingJobs.set(false);
      return;
    }

    this.hideLoadingTimeout = setTimeout(() => {
      this.loadingJobs.set(false);
      this.hideLoadingTimeout = null;
    }, remaining);
  }

  listJobsNewest(pageIndex: number, pageSize: number) {
    this.startJobsLoading();
    let params = new HttpParams()
      .set('page', pageIndex)
      .set('size', pageSize);
    this.http.get<ApiResponse<PagedPayload<JobCardModel>>>(
      `${this.url}/jobs/newest`, { params }
    ).pipe(
      take(1),
      finalize(() => this.finishJobsLoading())
    ).subscribe({
      next: (response) => {
        this.jobData.set(response.data.content);
        if (typeof response.data?.page?.totalElements === 'number') {
          this.totalJobData.set(response.data.page.totalElements);
        }
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }

  getAmount() {
    return this.http.get<JobCountApiResponse>(`${this.url}/jobs/count`).pipe(
      take(1),
      tap((response) => this.totalJobData.set(response.data))
    );
  }

  loadAddressCount(force = false): void {
    if (!force && this.addressData().length > 0) {
      return;
    }

    this.http.get<JobAddressCountApiResponse>(`${this.url}/jobs/address-count`).pipe(take(1)).subscribe({
      next: (response) => {
        this.addressData.set(response.data);
      },
      error: (error) => {
        console.error('Error fetching address count:', error);
      },
    });
  }

  getAddressCount(): void {
    this.loadAddressCount();
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload) {
    this.startJobsLoading();
    const normalizedFilter = this.cloneFilter(filter);
    this.setFilterPayload(normalizedFilter);
    this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, normalizedFilter).pipe(
      take(1),
      finalize(() => this.finishJobsLoading())
    ).subscribe({
      next: (response) => {
        this.jobData.set(response.data.content);
        if (typeof response.data?.page?.totalElements === 'number') {
          this.totalJobData.set(response.data.page.totalElements);
        }
      },
      error: (error) => {
        console.error('Error filtering jobs:', error);
      },
    });
  }
}
