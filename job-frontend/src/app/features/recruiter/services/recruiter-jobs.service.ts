import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { PagedPayload, HirerJobViewModel } from '../../../shared/models/jobs/job-api-response.model';

export type RecruiterJobViewModel = HirerJobViewModel;

@Injectable({
  providedIn: 'root'
})
export class RecruiterJobsService {
  private readonly url: string;

  private readonly postedJobs = signal<RecruiterJobViewModel[]>([]);
  private readonly postedJobsTotalCount = signal<number>(0);
  private readonly postedJobsTotalPages = signal<number>(0);
  private readonly isLoadingPostedJobs = signal<boolean>(false);

  readonly postedJobs$ = computed(() => this.postedJobs());
  readonly postedJobsTotalCount$ = computed(() => this.postedJobsTotalCount());
  readonly postedJobsTotalPages$ = computed(() => this.postedJobsTotalPages());
  readonly isLoadingPostedJobs$ = computed(() => this.isLoadingPostedJobs());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    this.url = this.utilities.getURLDev();
  }

  loadPostedJobs(pageIndex: number, pageSize: number): void {
    this.isLoadingPostedJobs.set(true);

    const params = new HttpParams()
      .set('page', pageIndex)
      .set('size', pageSize);

    this.http.get<ApiResponse<PagedPayload<RecruiterJobViewModel>>>(
      `${this.url}/hirer/jobs/posted`,
      { params, withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.postedJobs.set(response.data?.content ?? []);
        this.postedJobsTotalPages.set(response.data?.totalPages ?? 0);
        this.postedJobsTotalCount.set(response.data?.totalElements ?? 0);
        this.isLoadingPostedJobs.set(false);
      },
      error: () => {
        this.postedJobs.set([]);
        this.postedJobsTotalPages.set(0);
        this.postedJobsTotalCount.set(0);
        this.isLoadingPostedJobs.set(false);
        this.notify.error(this.i18nService.translate('recruiterCommon.errors.loadPostedJobsFailed'));
      }
    });
  }

  loadPostedJobCount(): void {
    this.http.get<ApiResponse<number>>(
      `${this.url}/hirer/jobs/posted/count`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.postedJobsTotalCount.set(response.data ?? 0);
      },
      error: () => {
        this.postedJobsTotalCount.set(0);
      }
    });
  }

  deletePostedJob(jobId: number): Observable<ApiResponse<string | null>> {
    return this.http.delete<ApiResponse<string | null>>(
      `${this.url}/hirer/jobs/${encodeURIComponent(String(jobId))}`,
      { withCredentials: true }
    ).pipe(take(1));
  }
}
