import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnInit, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { JobMatchCardComponent } from '../../../shared/components/job-match-card/job-match-card.component';
import { SkeletonCvCardComponent } from '../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../core/services/resume.service';
import { JobService } from '../../jobs/services/job.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-recommended-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, JobMatchCardComponent, SkeletonCvCardComponent, TranslatePipe],
  templateUrl: './recommended-jobs.component.html',
  styleUrl: './recommended-jobs.component.css'
})
export class RecommendedJobsComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);
  private readonly jobService = inject(JobService);

  readonly skeletonRows = [1, 2, 3];

  resumes: any[] = [];
  isResumeLoading = false;
  suggestedJobs: any[] = [];
  isJobLoading = false;
  readonly selectedResumeId = signal<number | null>(null);
  readonly isDropdownOpen = signal(false);

  selectedResume: any = null;
  suggestionCount = 0;

  constructor() {
    effect(() => {
      this.resumes = this.resumeService.analyzedResumes$();
      this.isResumeLoading = this.resumeService.isLoadingResumes$();
      this.suggestedJobs = this.jobService.recommendedJobs$();
      this.isJobLoading = this.jobService.isLoadingRecommendedJobs$();

      const resumeId = this.selectedResumeId();
      if (resumeId === null) {
        this.selectedResume = null;
      } else {
        this.selectedResume = this.resumes.find((resume) => resume.id === resumeId) ?? null;
      }
      this.suggestionCount = this.suggestedJobs.length;
    });
  }

  ngOnInit(): void {
    this.resumeService.getAnalyzedResumes();
  }

  onResumeChange(resumeId: number | null): void {
    if (resumeId === null) return;
    this.selectedResumeId.set(resumeId);
    this.jobService.getSuggestedJobsByResume(resumeId);
  }

  toggleDropdown(): void {
    if (this.isResumeLoading || this.resumes.length === 0) return;
    this.isDropdownOpen.update(v => !v);
  }

  selectResume(resumeId: number): void {
    this.onResumeChange(resumeId);
    this.isDropdownOpen.set(false);
  }
}
