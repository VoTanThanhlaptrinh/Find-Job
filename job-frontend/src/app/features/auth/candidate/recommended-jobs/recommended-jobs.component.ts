import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, finalize, of, Subject, switchMap, take } from 'rxjs';
import { CandidateJobSuggestionService } from '../../../../core/services/candidate-job-suggestion.service';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonCvCardComponent } from '../../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { ResumeReviewInput } from '../../../../shared/models/jobs/resume-review-input.model';

@Component({
  selector: 'app-recommended-jobs',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, JobCardComponent, SkeletonCvCardComponent],
  templateUrl: './recommended-jobs.component.html',
  styleUrl: './recommended-jobs.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendedJobsComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly resumeSelection$ = new Subject<number>();

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

  resumes: ResumeReviewInput[] = [];
  suggestedJobs: JobCardModel[] = [];
  selectedResumeId: number | null = null;
  isResumeLoading = false;
  isJobLoading = false;

  constructor(private readonly candidateJobSuggestionService: CandidateJobSuggestionService) {}

  ngOnInit(): void {
    this.bindResumeSelection();
    this.loadResumes();
  }

  get selectedResume(): ResumeReviewInput | null {
    if (this.selectedResumeId === null) {
      return null;
    }

    return this.resumes.find((resume) => resume.id === this.selectedResumeId) ?? null;
  }

  get selectedResumeDateLabel(): string {
    const resume = this.selectedResume;
    if (resume === null) {
      return '--';
    }

    const date = new Date(resume.createDate);
    if (Number.isNaN(date.getTime())) {
      return resume.createDate;
    }

    return new Intl.DateTimeFormat('vi-VN').format(date);
  }

  get suggestionCount(): number {
    return this.suggestedJobs.length;
  }

  onResumeChange(resumeId: number | null): void {
    if (resumeId === null) {
      return;
    }

    this.resumeSelection$.next(resumeId);
  }

  private bindResumeSelection(): void {
    this.resumeSelection$
      .pipe(
        distinctUntilChanged(),
        switchMap((resumeId) => {
          this.isJobLoading = true;
          this.selectedResumeId = resumeId;
          this.suggestedJobs = [];

          return this.candidateJobSuggestionService.getSuggestedJobsByResume(resumeId).pipe(
            take(1),
            catchError(() => of([] as JobCardModel[])),
            finalize(() => {
              this.isJobLoading = false;
            })
          );
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((jobs) => {
        this.suggestedJobs = jobs;
      });
  }

  private loadResumes(): void {
    this.isResumeLoading = true;

    this.candidateJobSuggestionService.getCandidateResumes().pipe(take(1)).subscribe({
      next: (resumes) => {
        this.resumes = resumes;
        this.isResumeLoading = false;

        if (resumes.length === 0) {
          this.selectedResumeId = null;
          this.suggestedJobs = [];
          return;
        }

        this.selectedResumeId = resumes[0].id;
        this.resumeSelection$.next(resumes[0].id);
      },
      error: () => {
        this.isResumeLoading = false;
        this.resumes = [];
        this.selectedResumeId = null;
        this.suggestedJobs = [];
      }
    });
  }
}
