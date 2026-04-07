import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { PagedPayload, HirerJobPostView } from '../../../shared/models/jobs/job-api-response.model';

export type RecruiterJobUpsertPayload = FormData;

export type RecruiterJobViewModel = HirerJobPostView;

@Injectable({
  providedIn: 'root'
})
export class RecruiterJobsService {
  private readonly url: string;

  private readonly postedJobs = signal<RecruiterJobViewModel[]>([]);
  private readonly postedJobsTotalCount = signal<number>(0);
  private readonly postedJobsTotalPages = signal<number>(0);
  private readonly isLoadingPostedJobs = signal<boolean>(false);
  private readonly isSubmittingJob = signal<boolean>(false);
  private readonly lastActionType = signal<'create' | 'update' | 'delete' | null>(null);
  private readonly lastActionSuccess = signal<boolean>(false);
  private readonly lastActionMessage = signal<string>('');
  private readonly actionTick = signal<number>(0);

  readonly postedJobs$ = computed(() => this.postedJobs());
  readonly postedJobsTotalCount$ = computed(() => this.postedJobsTotalCount());
  readonly postedJobsTotalPages$ = computed(() => this.postedJobsTotalPages());
  readonly isLoadingPostedJobs$ = computed(() => this.isLoadingPostedJobs());
  readonly isSubmittingJob$ = computed(() => this.isSubmittingJob());
  readonly lastActionType$ = computed(() => this.lastActionType());
  readonly lastActionSuccess$ = computed(() => this.lastActionSuccess());
  readonly lastActionMessage$ = computed(() => this.lastActionMessage());
  readonly actionTick$ = computed(() => this.actionTick());

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
    this.http.get<ApiResponse<number> | number | { count?: number; data?: number }>(
      `${this.url}/hirer/jobs/posted/count`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        const count = this.extractCount(response);
        this.postedJobsTotalCount.set(count);
      },
      error: () => {
        this.postedJobsTotalCount.set(0);
      }
    });
  }

  private extractCount(response: ApiResponse<number> | number | { count?: number; data?: number }): number {
    if (typeof response === 'number' && Number.isFinite(response)) {
      return response;
    }

    if (response && typeof response === 'object') {
      const dataValue = (response as { data?: unknown }).data;
      if (typeof dataValue === 'number' && Number.isFinite(dataValue)) {
        return dataValue;
      }

      const countValue = (response as { count?: unknown }).count;
      if (typeof countValue === 'number' && Number.isFinite(countValue)) {
        return countValue;
      }
    }

    return 0;
  }

  deletePostedJob(jobId: number): void {
    this.isSubmittingJob.set(true);
    this.lastActionType.set('delete');

    this.http.delete<ApiResponse<string | null>>(
      `${this.url}/hirer/jobs/${encodeURIComponent(String(jobId))}`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.lastActionSuccess.set(true);
        this.lastActionMessage.set(response?.message ?? 'Thao tác thành công.');
        this.actionTick.update((value) => value + 1);
      },
      error: (error: { error?: { message?: string } }) => {
        this.lastActionSuccess.set(false);
        this.lastActionMessage.set(error?.error?.message ?? 'Thao tác thất bại.');
        this.actionTick.update((value) => value + 1);
      },
      complete: () => {
        this.isSubmittingJob.set(false);
      }
    });
  }

  createJob(payload: RecruiterJobUpsertPayload): void {
    this.isSubmittingJob.set(true);
    this.lastActionType.set('create');

    this.http.post<ApiResponse<string | null>>(
      `${this.url}/hirer/jobs`,
      payload,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.lastActionSuccess.set(true);
        this.lastActionMessage.set(response?.message ?? 'Thao tác thành công.');
        this.actionTick.update((value) => value + 1);
      },
      error: (error: { error?: { message?: string } }) => {
        this.lastActionSuccess.set(false);
        this.lastActionMessage.set(error?.error?.message ?? 'Thao tác thất bại.');
        this.actionTick.update((value) => value + 1);
      },
      complete: () => {
        this.isSubmittingJob.set(false);
      }
    });
  }

  updateJob(jobId: number, payload: RecruiterJobUpsertPayload): void {
    this.isSubmittingJob.set(true);
    this.lastActionType.set('update');

    this.http.put<ApiResponse<string | null>>(
      `${this.url}/hirer/jobs/${encodeURIComponent(String(jobId))}`,
      payload,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.lastActionSuccess.set(true);
        this.lastActionMessage.set(response?.message ?? 'Thao tác thành công.');
        this.actionTick.update((value) => value + 1);
      },
      error: (error: { error?: { message?: string } }) => {
        this.lastActionSuccess.set(false);
        this.lastActionMessage.set(error?.error?.message ?? 'Thao tác thất bại.');
        this.actionTick.update((value) => value + 1);
      },
      complete: () => {
        this.isSubmittingJob.set(false);
      }
    });
  }
}
