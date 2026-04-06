import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { ResumeReviewComponent } from '../../../../shared/components/resume-review/resume-review.component';
import {
  CandidateStatus,
  RecruiterCandidateViewModel,
  RecruiterAccountService
} from '../../services/recruiter-account.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
import { RecruiterResumeService } from '../../services/recruiter-resume.service';

@Component({
  selector: 'app-candidate-list',
  standalone: true,
  imports: [CommonModule, TranslatePipe, ResumeReviewComponent],
  templateUrl: './candidate-list.component.html',
  styleUrl: './candidate-list.component.css'
})
export class CandidateListComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap
  });

  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(10);
  readonly selectedJobId = signal<number | null>(null);
  readonly selectedCandidateEmail = signal<string>('');

  constructor(
    private readonly recruiterAccountService: RecruiterAccountService,
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly recruiterResumeService: RecruiterResumeService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    this.recruiterJobsService.loadPostedJobs(0, 20);

    effect(() => {
      const queryParams = this.queryParamMap();
      const queryJobId = Number(queryParams.get('jobId'));
      if (Number.isFinite(queryJobId) && queryJobId > 0) {
        this.selectedJobId.set(queryJobId);
      }
    });

    effect(() => {
      const jobs = this.recruiterJobsService.postedJobs$();
      if (!this.selectedJobId() && jobs.length > 0) {
        const firstJobId = Number(jobs[0].id);
        if (Number.isFinite(firstJobId) && firstJobId > 0) {
          this.selectedJobId.set(firstJobId);
        }
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

  get jobs() {
    return this.recruiterJobsService.postedJobs$();
  }

  get resumes() {
    return this.recruiterResumeService.candidateResumes$();
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

  onSelectJob(event: Event): void {
    const selected = Number((event.target as HTMLSelectElement).value);
    if (!Number.isFinite(selected) || selected <= 0) {
      this.selectedJobId.set(null);
      return;
    }

    this.pageIndex.set(0);
    this.selectedJobId.set(selected);
    this.selectedCandidateEmail.set('');
    this.recruiterResumeService.clearCandidateResumes();
  }

  viewCandidateResumes(candidate: RecruiterCandidateViewModel): void {
    if (!candidate.email) {
      this.notify.warning(this.i18nService.translate('candidateList.notifications.candidateEmailMissing'));
      return;
    }

    this.selectedCandidateEmail.set(candidate.email);
    this.recruiterResumeService.loadResumesByCandidateEmail(candidate.email);
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
}
