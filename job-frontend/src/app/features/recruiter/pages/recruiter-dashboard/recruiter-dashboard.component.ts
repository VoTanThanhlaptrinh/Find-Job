import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TokenService } from '../../../../core/services/token.service';
import { HirerJobPostView } from '../../../../shared/models/jobs/job-api-response.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';

@Component({
  selector: 'app-recruiter-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrl: './recruiter-dashboard.component.css',
})
export class RecruiterDashboardComponent implements OnInit {
  private readonly recruiterJobsService = inject(RecruiterJobsService);
  private readonly tokenService = inject(TokenService);
  private readonly jobsPageSize = 12;

  readonly recruiterName = this.tokenService.getTokenSubject() || 'Recruiter';
  readonly jobs = this.recruiterJobsService.postedJobs$;
  readonly totalJobs = this.recruiterJobsService.postedJobsTotalCount$;
  readonly isLoading = this.recruiterJobsService.isLoadingPostedJobs$;
  readonly hasLoadError = this.recruiterJobsService.hasPostedJobsError$;

  readonly jobsWithApplicants = computed(() =>
    this.jobs().filter((job) => job.applies > 0).length
  );

  readonly jobsWithoutApplicants = computed(() =>
    this.jobs().filter((job) => job.applies === 0).length
  );

  readonly unanalyzedJobs = computed(() =>
    this.jobs().filter((job) => !job.isAnalyzed).length
  );

  readonly totalApplicationsInView = computed(() =>
    this.jobs().reduce((total, job) => total + Math.max(job.applies ?? 0, 0), 0)
  );

  readonly priorityJobs = computed(() =>
    [...this.jobs()]
      .sort((left, right) => this.priorityScore(right) - this.priorityScore(left))
      .slice(0, 5)
  );

  ngOnInit(): void {
    this.refreshDashboard();
  }

  refreshDashboard(): void {
    this.recruiterJobsService.loadPostedJobs(0, this.jobsPageSize);
  }

  analyzeJob(job: HirerJobPostView): void {
    if (!job.isAnalyzed) {
      this.recruiterJobsService.analyzeJob(job.id);
    }
  }

  trackByJobId(_: number, job: HirerJobPostView): number {
    return job.id;
  }

  private priorityScore(job: HirerJobPostView): number {
    if (!job.isAnalyzed && job.applies === 0) {
      return 3;
    }
    if (job.applies === 0) {
      return 2;
    }

    return job.isAnalyzed ? 0 : 1;
  }
}
