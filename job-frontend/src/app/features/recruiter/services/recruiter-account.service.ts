import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { take } from 'rxjs';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { PagedPayload } from '../../../shared/models/jobs/job-api-response.model';

export type CandidateStatus = 'new' | 'interviewing' | 'shortlisted' | 'rejected';

export interface RecruiterCandidateViewModel {
  id: number;
  name: string;
  email: string;
  role: string;
  experience: string;
  status: CandidateStatus;
}

interface RecruiterCandidatesApiRaw {
  id?: number;
  userId?: number;
  userName?: string;
  fullName?: string;
  candidateName?: string;
  email?: string;
  userEmail?: string;
  role?: string;
  jobTitle?: string;
  experience?: string | number;
  yearsOfExperience?: number;
  status?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RecruiterAccountService {
  private readonly url: string;

  private readonly candidates = signal<RecruiterCandidateViewModel[]>([]);
  private readonly candidatesTotal = signal<number>(0);
  private readonly candidatesTotalPages = signal<number>(0);
  private readonly isLoadingCandidates = signal<boolean>(false);

  readonly candidates$ = computed(() => this.candidates());
  readonly candidatesTotal$ = computed(() => this.candidatesTotal());
  readonly candidatesTotalPages$ = computed(() => this.candidatesTotalPages());
  readonly isLoadingCandidates$ = computed(() => this.isLoadingCandidates());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    this.url = this.utilities.getURLDev();
  }

  loadCandidatesByJob(jobId: number, pageIndex: number, pageSize: number): void {
    this.isLoadingCandidates.set(true);

    this.http.get<ApiResponse<PagedPayload<RecruiterCandidatesApiRaw>>>(
      `${this.url}/hirer/applications/jobs/${encodeURIComponent(String(jobId))}/candidates/${encodeURIComponent(String(pageIndex))}/${encodeURIComponent(String(pageSize))}`,
      { withCredentials: true }
    ).pipe(take(1)).subscribe({
      next: (response) => {
        const content = response.data?.content ?? [];
        this.candidates.set(content.map((candidate) => this.mapCandidate(candidate)));
        this.candidatesTotal.set(response.data?.totalElements ?? 0);
        this.candidatesTotalPages.set(response.data?.totalPages ?? 0);
        this.isLoadingCandidates.set(false);
      },
      error: () => {
        this.candidates.set([]);
        this.candidatesTotal.set(0);
        this.candidatesTotalPages.set(0);
        this.isLoadingCandidates.set(false);
        this.notify.error(this.i18nService.translate('recruiterCommon.errors.loadCandidatesFailed'));
      }
    });
  }

  private mapCandidate(candidate: RecruiterCandidatesApiRaw): RecruiterCandidateViewModel {
    const id = Number(candidate.id ?? candidate.userId ?? 0);
    const name = candidate.fullName
      ?? candidate.userName
      ?? candidate.candidateName
      ?? this.i18nService.translate('recruiterCommon.fallback.unknownName');
    const email = candidate.email ?? candidate.userEmail ?? '';
    const role = candidate.role
      ?? candidate.jobTitle
      ?? this.i18nService.translate('recruiterCommon.fallback.notAvailable');

    let experience = this.i18nService.translate('recruiterCommon.fallback.notAvailable');
    if (typeof candidate.experience === 'number') {
      experience = `${candidate.experience} ${this.i18nService.translate('recruiterCommon.fallback.years')}`;
    } else if (typeof candidate.experience === 'string' && candidate.experience.trim().length > 0) {
      experience = candidate.experience;
    } else if (typeof candidate.yearsOfExperience === 'number') {
      experience = `${candidate.yearsOfExperience} ${this.i18nService.translate('recruiterCommon.fallback.years')}`;
    }

    return {
      id,
      name,
      email,
      role,
      experience,
      status: this.mapCandidateStatus(candidate.status)
    };
  }

  private mapCandidateStatus(status?: string): CandidateStatus {
    const normalized = (status ?? '').trim().toLowerCase();
    if (normalized.includes('interview')) {
      return 'interviewing';
    }
    if (normalized.includes('short')) {
      return 'shortlisted';
    }
    if (normalized.includes('reject')) {
      return 'rejected';
    }
    return 'new';
  }
}
