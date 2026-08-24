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
  pageSize: 10,
  address: [],
  times: [],
  title: '',
  categoryIds: [],
};

@Injectable({
  providedIn: 'root',
})
export class FilterService {
  private static readonly MIN_LOADING_MS = 600;
  private url: string;

  private jobData = signal<JobCardModel[]>([]);
  private addressData = signal<AddressCountViewModel[]>([]);
  private totalJobData = signal<number | null>(null);
  private loadingJobs = signal(false);
  private errorState = signal(false);
  private filterPayload = signal<JobFilterPayload>({ ...DEFAULT_JOB_FILTER });

  private activeJobRequests = 0;
  private loadingStartAt = 0;
  private hideLoadingTimeout: ReturnType<typeof setTimeout> | null = null;

  jobs = computed(() => this.jobData());
  addressCount = computed(() => this.addressData());
  totalJobs = computed(() => this.totalJobData());
  isLoadingJobs = computed(() => this.loadingJobs());
  hasError = computed(() => this.errorState());
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
      categoryIds: Array.isArray(filter.categoryIds) ? [...filter.categoryIds] : [],
    };
  }

  private startJobsLoading(): void {
    this.errorState.set(false);
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
    const remaining = Math.max(0, FilterService.MIN_LOADING_MS - elapsed);

    if (remaining === 0) {
      this.loadingJobs.set(false);
      return;
    }

    this.hideLoadingTimeout = setTimeout(() => {
      this.loadingJobs.set(false);
      this.hideLoadingTimeout = null;
    }, remaining);
  }

  sortJobs(jobs: JobCardModel[], sortType: string): JobCardModel[] {
    if (!jobs || jobs.length === 0) return [];
    const list = [...jobs];

    if (sortType === 'salary-desc') {
      return list.sort((a, b) => this.extractSalaryValue(b.salary) - this.extractSalaryValue(a.salary));
    } else if (sortType === 'salary-asc') {
      return list.sort((a, b) => this.extractSalaryValue(a.salary) - this.extractSalaryValue(b.salary));
    }

    return list;
  }

  private extractSalaryValue(salary: number | string | undefined | null): number {
    if (salary === undefined || salary === null) return 0;
    if (typeof salary === 'number') return salary;

    const str = String(salary).toLowerCase().trim();
    if (str.includes('thỏa thuận') || str === '') return 0;

    const numbers = str.match(/\d+(\.\d+)?/g);
    if (!numbers || numbers.length === 0) return 0;

    const lastNum = parseFloat(numbers[numbers.length - 1]);
    if (str.includes('triệu') || str.includes('tr')) {
      return lastNum * 1000000;
    } else if (str.includes('k') || str.includes('nghìn')) {
      return lastNum * 1000;
    }

    return lastNum;
  }

  listJobsNewest(pageIndex: number, pageSize: number, sortType = 'relevant') {
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
        const content = response.data?.content || [];
        this.jobData.set(this.sortJobs(content, sortType));
        if (typeof response.data?.page?.totalElements === 'number') {
          this.totalJobData.set(response.data.page.totalElements);
        } else {
          this.totalJobData.set(content.length);
        }
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
        this.errorState.set(true);
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

    this.http.get<JobAddressCountApiResponse>(`${this.url}/addresses/address-count`).pipe(take(1)).subscribe({
      next: (response) => {
        this.addressData.set(response.data || []);
      },
      error: (error) => {
        console.error('Error fetching address count:', error);
      },
    });
  }

  getAddressCount(): void {
    this.loadAddressCount();
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload, sortType = 'relevant') {
    this.startJobsLoading();
    const normalizedFilter = this.cloneFilter(filter);
    this.setFilterPayload(normalizedFilter);
    this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, normalizedFilter).pipe(
      take(1),
      finalize(() => this.finishJobsLoading())
    ).subscribe({
      next: (response) => {
        const content = response.data?.content || [];
        this.jobData.set(this.sortJobs(content, sortType));
        if (typeof response.data?.page?.totalElements === 'number') {
          this.totalJobData.set(response.data.page.totalElements);
        } else {
          this.totalJobData.set(content.length);
        }
      },
      error: (error) => {
        console.error('Error filtering jobs:', error);
        this.errorState.set(true);
      },
    });
  }
}
