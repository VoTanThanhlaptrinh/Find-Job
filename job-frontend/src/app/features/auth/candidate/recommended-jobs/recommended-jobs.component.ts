import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonCvCardComponent } from '../../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../../core/services/resume.service';
import { JobService } from '../../../jobs/services/job.service';

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

  readonly title = 'Gợi ý các công việc phù hợp.';
  readonly selectLabel = 'Chọn CV';
  readonly selectEmptyLabel = 'Bạn chưa có CV nào';
  readonly selectLoadingLabel = 'Đang tải danh sách CV...';
  readonly helperText = 'Danh sách job sẽ thay đổi theo CV bạn đang chọn.';
  readonly selectedResumeLabel = 'CV đang phân tích';
  readonly selectedResumeDatePrefix = 'Cập nhật';
  readonly suggestionCountLabel = 'Công việc phù hợp';
  readonly emptyResumeTitle = 'Bạn chưa có CV để hệ thống gợi ý.';
  readonly emptyResumeDescription = 'Hãy tải CV lên trước, sau đó quay lại tab này để xem danh sách công việc phù hợp.';
  readonly emptyResumeAction = 'Quản lý CV';
  readonly emptyJobTitle = 'Chưa có công việc phù hợp cho CV này.';
  readonly emptyJobDescription = 'Bạn có thể chọn CV khác hoặc cập nhật nội dung CV để nhận gợi ý tốt hơn.';
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

  readonly selectedResumeDateLabel = computed(() => {
    const resume = this.selectedResume();
    if (resume === null) return '--';
    const date = new Date(resume.createDate);
    if (Number.isNaN(date.getTime())) return resume.createDate;
    return new Intl.DateTimeFormat('vi-VN').format(date);
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
