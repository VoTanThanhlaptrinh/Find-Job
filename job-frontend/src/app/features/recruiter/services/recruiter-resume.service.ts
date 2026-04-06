import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { map, Observable, take } from 'rxjs';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { ResumeReviewInput } from '../../../shared/models/jobs/resume-review-input.model';
import { ResumeUrlDTO } from '../../../shared/models/jobs/resume-url-dto.model';

@Injectable({
  providedIn: 'root'
})
export class RecruiterResumeService {
  private readonly url: string;

  private readonly candidateResumes = signal<ResumeReviewInput[]>([]);
  private readonly isLoadingCandidateResumes = signal<boolean>(false);

  readonly candidateResumes$ = computed(() => this.candidateResumes());
  readonly isLoadingCandidateResumes$ = computed(() => this.isLoadingCandidateResumes());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    this.url = this.utilities.getURLDev();
  }

  loadResumesByCandidateEmail(email: string): void {
    this.isLoadingCandidateResumes.set(true);

    this.http.get<ApiResponse<ResumeReviewInput[]>>(
      `${this.url}/hirer/resumes/users/${encodeURIComponent(email)}`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        this.candidateResumes.set(response.data ?? []);
        this.isLoadingCandidateResumes.set(false);
      },
      error: () => {
        this.candidateResumes.set([]);
        this.isLoadingCandidateResumes.set(false);
        this.notify.error(this.i18nService.translate('recruiterCommon.errors.loadResumesFailed'));
      }
    });
  }

  clearCandidateResumes(): void {
    this.candidateResumes.set([]);
  }

  getRecruiterResumeViewUrl(resumeId: number): Observable<string> {
    return this.http.get<ApiResponse<ResumeUrlDTO>>(
      `${this.url}/hirer/resumes/${encodeURIComponent(String(resumeId))}/view`,
      { withCredentials: true }
    ).pipe(
      take(1),
      map((response) => response.data.url)
    );
  }

  getRecruiterResumeDownloadUrl(resumeId: number): Observable<string> {
    return this.http.get<ApiResponse<ResumeUrlDTO>>(
      `${this.url}/hirer/resumes/${encodeURIComponent(String(resumeId))}/download`,
      { withCredentials: true }
    ).pipe(
      take(1),
      map((response) => response.data.url)
    );
  }
}
