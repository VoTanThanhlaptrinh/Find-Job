import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { JobCardComponent } from '../../../shared/components/job-card/job-card.component';
import { SkeletonCvCardComponent } from '../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../core/services/resume.service';
import { JobService } from '../../jobs/services/job.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';


@Component({
  selector: 'app-recommended-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, JobCardComponent, SkeletonCvCardComponent, TranslatePipe],
  templateUrl: './recommended-jobs.component.html',
  styleUrl: './recommended-jobs.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendedJobsComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);
  private readonly jobService = inject(JobService);

  readonly skeletonRows = [1, 2, 3];

  readonly resumes = this.resumeService.analyzedResumes$;
  readonly isResumeLoading = this.resumeService.isLoadingResumes$;
  readonly suggestedJobs = this.jobService.recommendedJobs$;
  readonly isJobLoading = this.jobService.isLoadingRecommendedJobs$;
  readonly selectedResumeId = signal<number | null>(null);
  readonly isDropdownOpen = signal(false);

  readonly selectedResume = computed(() => {
    const resumeId = this.selectedResumeId();
    if (resumeId === null) return null;
    return this.resumes().find((resume) => resume.id === resumeId) ?? null;
  });

  readonly suggestionCount = computed(() => this.suggestedJobs().length);

  constructor() {
    effect(() => {
      const resumeId = this.selectedResumeId();
      if (resumeId !== null) {
        this.jobService.getSuggestedJobsByResume(resumeId);
      }
    });
  }

  ngOnInit(): void {
    this.resumeService.getAnalyzedResumes();
  }

  onResumeChange(resumeId: number | null): void {
    if (resumeId === null) return;
    this.selectedResumeId.set(resumeId);
  }

  toggleDropdown(): void {
    if (this.isResumeLoading() || this.resumes().length === 0) return;
    this.isDropdownOpen.update(v => !v);
  }

  selectResume(resumeId: number): void {
    this.onResumeChange(resumeId);
    this.isDropdownOpen.set(false);
  }
}
