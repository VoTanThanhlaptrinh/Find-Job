import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, Observable, take } from 'rxjs';
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

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private static readonly MIN_LOADING_MS = 1000;

  private url: string;
  private jobData = signal<JobCardModel[]>([]);
  private addressData = signal<AddressCountViewModel[]>([]);
  private loadingJobs = signal(false);
  private activeJobRequests = 0;
  private loadingStartAt = 0;
  private hideLoadingTimeout: ReturnType<typeof setTimeout> | null = null;

  jobs = computed(() => this.jobData());
  addressCount = computed(() => this.addressData());
  isLoadingJobs = computed(() => this.loadingJobs());

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
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
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }

  getAmount() {
    return this.http.get<JobCountApiResponse>(`${this.url}/jobs/count`).pipe(take(1));
  }

  getAddressCount() {
    this.http.get<JobAddressCountApiResponse>(`${this.url}/jobs/address-count`).pipe(take(1)).subscribe({
      next: (response) => {
        this.addressData.set(response.data);
      },
      error: (error) => {
        console.error('Error fetching address count:', error);
      },
    });
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload) {
    this.startJobsLoading();
    this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, filter).pipe(
      take(1),
      finalize(() => this.finishJobsLoading())
    ).subscribe({
      next: (response) => {
        this.jobData.set(response.data.content);
      },
      error: (error) => {
        console.error('Error filtering jobs:', error);
      },
    });
  }
}
