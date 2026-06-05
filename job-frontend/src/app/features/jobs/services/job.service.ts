import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import {
  ApplyCvWithExistingRequest,
  ApplyCvWithExistingResponse,
  ApplyCvWithUploadRequest,
  ApplyCvWithUploadResponse,
} from '../../../shared/models/jobs/apply-cv.model';
import {
  HirerJobCountApiResponse,
  HirerJobListApiResponse,
  JobDetailApiResponse,
  JobExistsApiResponse,
  JobFilterPayload,
  JobListApiResponse,
  JobSubmitApiResponse,
  JobDetailViewModel,
} from '../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../shared/models/jobs/job-card.model';
import { ApiResponse } from '../../../shared/models/api-response.model';

const INITIAL_JOB_DETAIL: JobDetailViewModel = {
  id: '',
  title: '',
  address: '',
  description: '',
  salary: 0,
  time: '',
  requireDetails: '',
  skill: '',
  expiredDate: '',
  headcount: 0,
};

@Injectable({
  providedIn: 'root',
})
export class JobService {
  private jobDetail = signal<JobDetailViewModel | null>(null);
  private jobDetailError = signal<boolean>(false);
  private hasApplied = signal<boolean | null>(null);
  private isCheckingApply = signal<boolean>(false);
  private checkApplyErrorStatus = signal<number | null>(null);
  private recommendedJobs = signal<JobCardModel[]>([]);
  private isLoadingRecommendedJobs = signal<boolean>(false);
  private appliedJobs = signal<JobCardModel[]>([]);
  private isLoadingAppliedJobs = signal<boolean>(false);
  private hasMoreAppliedJobs = signal<boolean>(true);
  private nextAppliedJobsPage = signal<number>(0);

  readonly jobDetail$ = computed(() => this.jobDetail());
  readonly jobDetailError$ = computed(() => this.jobDetailError());
  readonly hasApplied$ = computed(() => this.hasApplied());
  readonly isCheckingApply$ = computed(() => this.isCheckingApply());
  readonly checkApplyErrorStatus$ = computed(() => this.checkApplyErrorStatus());
  readonly recommendedJobs$ = computed(() => this.recommendedJobs());
  readonly isLoadingRecommendedJobs$ = computed(() => this.isLoadingRecommendedJobs());
  readonly appliedJobs$ = computed(() => this.appliedJobs());
  readonly isLoadingAppliedJobs$ = computed(() => this.isLoadingAppliedJobs());
  readonly hasMoreAppliedJobs$ = computed(() => this.hasMoreAppliedJobs());

  private url: string;

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  resetJobDetailState(): void {
    this.jobDetail.set(null);
    this.jobDetailError.set(false);
  }

  resetCheckApplyState(): void {
    this.hasApplied.set(null);
    this.isCheckingApply.set(false);
    this.checkApplyErrorStatus.set(null);
  }

  getDetailJob(id: string): void {
    this.resetJobDetailState();
    this.http.get<JobDetailApiResponse>(`${this.url}/jobs/${id}`).pipe(take(1)).subscribe({
      next: (response) => {
        this.jobDetail.set(response.data);
      },
      error: (error) => {
        this.jobDetail.set(INITIAL_JOB_DETAIL);
        this.jobDetailError.set(true);
        console.error('Error fetching job details:', error);
      }
    });
  }

  checkApplyJob(id: number): void {
    this.resetCheckApplyState();
    this.isCheckingApply.set(true);
    this.http.get<JobExistsApiResponse>(
      `${this.url}/user/applications/${id}/status`,
      { withCredentials: true }
    ).pipe(
      take(1),
      finalize(() => this.isCheckingApply.set(false))
    ).subscribe({
      next: (response) => {
        this.hasApplied.set(response.data);
      },
      error: (error: { status?: number }) => {
        this.hasApplied.set(false);
        this.checkApplyErrorStatus.set(error?.status ?? null);
      }
    });
  }

  getSuggestedJobsByResume(resumeId: number): void {
    this.isLoadingRecommendedJobs.set(true);
    this.recommendedJobs.set([]);
    this.http.get<ApiResponse<JobCardModel[]>>(`${this.url}/jobs/match/${resumeId}`)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.recommendedJobs.set(response.data);
          this.isLoadingRecommendedJobs.set(false);
        },
        error: () => {
          this.recommendedJobs.set([]);
          this.isLoadingRecommendedJobs.set(false);
        }
      });
  }

  clearRecommendedJobs(): void {
    this.recommendedJobs.set([]);
  }

  resetAppliedJobsPagination(): void {
    this.appliedJobs.set([]);
    this.hasMoreAppliedJobs.set(true);
    this.nextAppliedJobsPage.set(0);
    this.isLoadingAppliedJobs.set(false);
  }

  loadMoreAppliedJobs(pageSize: number = 10): void {
    if (this.isLoadingAppliedJobs() || !this.hasMoreAppliedJobs()) {
      return;
    }

    const pageToLoad = this.nextAppliedJobsPage();
    this.isLoadingAppliedJobs.set(true);

    this.listJobUserApplied(pageToLoad, pageSize)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          const content = response.data?.content ?? [];
          const totalPages = response.data?.page?.totalPages;
          const currentPage = response.data?.page?.number ?? pageToLoad;
          const isLastPageFromApi = response.data?.page?.number === response.data?.page?.totalPages;

          this.appliedJobs.update((currentJobs) => [...currentJobs, ...content]);

          const isLastPage = typeof isLastPageFromApi === 'boolean'
            ? isLastPageFromApi
            : (typeof totalPages === 'number' ? currentPage >= totalPages - 1 : content.length < pageSize);

          this.hasMoreAppliedJobs.set(!isLastPage && content.length > 0);
          this.nextAppliedJobsPage.set(pageToLoad + 1);
        },
        error: () => {
          this.hasMoreAppliedJobs.set(false);
        },
        complete: () => {
          this.isLoadingAppliedJobs.set(false);
        }
      });
  }

  submitApplyCvExisting(
    payload: ApplyCvWithExistingRequest
  ): Observable<ApplyCvWithExistingResponse> {
    return this.http.post<ApplyCvWithExistingResponse>(
      `${this.url}/user/applications/submit-existing`,
      payload,
      { withCredentials: true }
    ).pipe(take(1));
  }

  submitApplyCvUpload(
    payload: ApplyCvWithUploadRequest
  ): Observable<ApplyCvWithUploadResponse> {
    const formData = new FormData();
    formData.append('jobId', payload.jobId.toString());
    formData.append('cvFile', payload.cvFile);
    formData.append('coverLetter', payload.coverLetter);

    return this.http.post<ApplyCvWithUploadResponse>(
      `${this.url}/user/applications/submit-upload`,
      formData,
      { withCredentials: true }
    ).pipe(take(1));
  }

  doPostJob(form: FormData): Observable<JobSubmitApiResponse> {
    return this.http.post<JobSubmitApiResponse>(`${this.url}/hirer/jobs`, form, {
      withCredentials: true,
    }).pipe(take(1));
  }

  updateHirerJob(id: number, form: FormData): Observable<JobSubmitApiResponse> {
    return this.http.put<JobSubmitApiResponse>(
      `${this.url}/hirer/jobs/${encodeURIComponent(String(id))}`,
      form,
      { withCredentials: true }
    ).pipe(take(1));
  }

  deleteHirerJob(id: number): Observable<JobSubmitApiResponse> {
    return this.http.delete<JobSubmitApiResponse>(
      `${this.url}/hirer/jobs/${encodeURIComponent(String(id))}`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  getHirerJobPost(pageIndex: number, pageSize: number): Observable<HirerJobListApiResponse> {
    let params = new HttpParams()
      .set('page', pageIndex)
      .set('size', pageSize);
    return this.http.get<HirerJobListApiResponse>(
      `${this.url}/hirer/jobs/posted`,
      { params, withCredentials: true }
    ).pipe(take(1));
  }

  countHirerJobPost(): Observable<HirerJobCountApiResponse> {
    return this.http.get<HirerJobCountApiResponse>(
      `${this.url}/hirer/jobs/posted/count`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload): Observable<JobListApiResponse> {
    return this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, filter).pipe(take(1));
  }

  listJobUserApplied(pageIndex: number, pageSize: number): Observable<JobListApiResponse> {
    let params = new HttpParams()
      .set('page', pageIndex)
      .set('size', pageSize);
    return this.http.get<JobListApiResponse>(
      `${this.url}/user/applied`,
      { params, withCredentials: true }
    ).pipe(take(1));
  }
}
