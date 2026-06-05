import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { JobCardComponent } from '../../../shared/components/job-card/job-card.component';
import { SkeletonCvCardComponent } from '../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../core/services/resume.service';
import { JobService } from '../../jobs/services/job.service';
import { I18nService } from '../../../core/i18n/i18n.service';


@Component({
  selector: 'app-recommended-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, JobCardComponent, SkeletonCvCardComponent],
  templateUrl: './recommended-jobs.component.html',
  styleUrl: './recommended-jobs.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendedJobsComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);
  private readonly jobService = inject(JobService);
  private readonly i18n = inject(I18nService);

  readonly title = computed(() => this.i18n.translate('recommendedJobs.title'));
  readonly helperText = computed(() => this.i18n.translate('recommendedJobs.helperText'));
  readonly selectLabel = computed(() => this.i18n.translate('recommendedJobs.selectLabel'));
  readonly selectEmptyLabel = computed(() => this.i18n.translate('recommendedJobs.selectEmptyLabel'));
  readonly selectLoadingLabel = computed(() => this.i18n.translate('recommendedJobs.selectLoadingLabel'));
  readonly emptyResumeTitle = computed(() => this.i18n.translate('recommendedJobs.emptyResumeTitle'));
  readonly emptyResumeDescription = computed(() => this.i18n.translate('recommendedJobs.emptyResumeDescription'));
  readonly emptyResumeAction = computed(() => this.i18n.translate('recommendedJobs.emptyResumeAction'));
  readonly emptyJobTitle = computed(() => this.i18n.translate('recommendedJobs.emptyJobTitle'));
  readonly emptyJobDescription = computed(() => this.i18n.translate('recommendedJobs.emptyJobDescription'));
  readonly aiMatchingLabel = computed(() => this.i18n.translate('recommendedJobs.aiMatching'));
  readonly matchScoreLabel = computed(() => this.i18n.translate('recommendedJobs.matchScore'));
  readonly viewDetailLabel = computed(() => this.i18n.translate('recommendedJobs.viewDetail'));
  readonly selectOtherResumeLabel = computed(() => this.i18n.translate('recommendedJobs.selectOtherResume'));
  readonly aiAnalysisHintLabel = computed(() => this.i18n.translate('recommendedJobs.aiAnalysisHint'));

  readonly skeletonRows = [1, 2, 3];

  readonly resumes = this.resumeService.resumes$;
  readonly isResumeLoading = this.resumeService.isLoadingResumes$;
  readonly suggestedJobs = this.jobService.recommendedJobs$;
  readonly isJobLoading = this.jobService.isLoadingRecommendedJobs$;
  readonly selectedResumeId = signal<number | null>(null);

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
    this.resumeService.getUserResumes();
  }

  onResumeChange(resumeId: number | null): void {
    if (resumeId === null) return;
    this.selectedResumeId.set(resumeId);
  }
}
