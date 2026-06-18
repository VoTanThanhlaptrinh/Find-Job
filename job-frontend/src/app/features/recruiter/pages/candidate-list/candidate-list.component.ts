import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  CandidateStatus,
  RecruiterCandidateViewModel,
  RecruiterAccountService
} from '../../services/recruiter-account.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
import { RecruiterResumeService } from '../../services/recruiter-resume.service';
import { JobService } from '../../../jobs/services/job.service';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { take } from 'rxjs';

import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-candidate-list',
  standalone: true,
  imports: [CommonModule, TranslatePipe, RouterModule],
  templateUrl: './candidate-list.component.html',
  styleUrl: './candidate-list.component.css'
})
export class CandidateListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly paramMap = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap
  });

  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(10);
  readonly selectedJobId = signal<number | null>(null);
  readonly selectedCandidateEmail = signal<string>('');

  readonly jobDetail = signal<JobDetailViewModel | null>(null);

  constructor(
    private readonly recruiterAccountService: RecruiterAccountService,
    private readonly recruiterResumeService: RecruiterResumeService,
    private readonly jobService: JobService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    effect(() => {
      const params = this.paramMap();
      const jobIdStr = params.get('jobId');
      if (jobIdStr) {
        const jobId = Number(jobIdStr);
        if (Number.isFinite(jobId) && jobId > 0) {
          this.selectedJobId.set(jobId);
          this.jobService.getDetailJob(jobIdStr);
        }
      }
    });

    effect(() => {
      const detail = this.jobService.jobDetail$();
      if (detail && Number(detail.id) === this.selectedJobId()) {
        this.jobDetail.set(detail);
      }
    });

    effect(() => {
      const jobId = this.selectedJobId();
      const page = this.pageIndex();
      const size = this.pageSize();

      if (!jobId) {
        return;
      }

      this.recruiterAccountService.loadCandidatesByJob(jobId, page, size);
    });
  }

  get candidates(): RecruiterCandidateViewModel[] {
    return this.recruiterAccountService.candidates$();
  }



  get resumes() {
    return this.recruiterResumeService.candidateResumes$();
  }

  get totalCandidates(): number {
    return this.recruiterAccountService.candidatesTotal$();
  }

  get currentPage(): number {
    return this.pageIndex() + 1;
  }

  get totalPages(): number {
    return Math.max(this.recruiterAccountService.candidatesTotalPages$(), 1);
  }

  get canGoPrev(): boolean {
    return this.pageIndex() > 0;
  }

  get canGoNext(): boolean {
    return this.pageIndex() + 1 < Math.max(this.recruiterAccountService.candidatesTotalPages$(), 1);
  }

  get isLoadingCandidates(): boolean {
    return this.recruiterAccountService.isLoadingCandidates$();
  }

  get isLoadingResumes(): boolean {
    return this.recruiterResumeService.isLoadingCandidateResumes$();
  }

  get interviewingCount(): number {
    return this.candidates.filter((candidate) => candidate.status === 'interviewing').length;
  }

  get newThisWeekCount(): number {
    return this.candidates.filter((candidate) => candidate.status === 'new').length;
  }

  trackByCandidate(index: number, candidate: RecruiterCandidateViewModel): string {
    return `${candidate.name}-${candidate.role}-${index}`;
  }



  viewCandidateResumes(candidate: RecruiterCandidateViewModel): void {
    if (!candidate.email) {
      this.notify.warning(this.i18nService.translate('candidateList.notifications.candidateEmailMissing'));
      return;
    }

    this.selectedCandidateEmail.set(candidate.email);
    this.recruiterResumeService.loadResumesByCandidateEmail(candidate.email);
  }

  goPrevPage(): void {
    if (!this.canGoPrev) {
      return;
    }

    this.pageIndex.update((value) => value - 1);
  }

  goNextPage(): void {
    if (!this.canGoNext) {
      return;
    }

    this.pageIndex.update((value) => value + 1);
  }

  openResumeView(resumeId: number): void {
    this.recruiterResumeService.getRecruiterResumeViewUrl(resumeId).pipe(take(1)).subscribe({
      next: (url) => {
        this.openExternalUrl(url);
      },
      error: () => {
        this.notify.error(this.i18nService.translate('recruiterCommon.errors.loadResumesFailed'));
      }
    });
  }

  downloadResume(resumeId: number): void {
    this.recruiterResumeService.getRecruiterResumeDownloadUrl(resumeId).pipe(take(1)).subscribe({
      next: (url) => {
        this.openExternalUrl(url);
      },
      error: () => {
        this.notify.error(this.i18nService.translate('recruiterCommon.errors.loadResumesFailed'));
      }
    });
  }

  statusClass(status: CandidateStatus): string {
    switch (status) {
      case 'new':
        return 'bg-sky-100 text-sky-700';
      case 'interviewing':
        return 'bg-amber-100 text-amber-700';
      case 'shortlisted':
        return 'bg-emerald-100 text-emerald-700';
      case 'rejected':
      default:
        return 'bg-rose-100 text-rose-700';
    }
  }

  private openExternalUrl(url: string): void {
    if (!url) {
      return;
    }

    if (typeof window === 'undefined') {
      return;
    }

    window.open(url, '_blank', 'noopener');
  }
}
