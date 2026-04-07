import { CommonModule } from '@angular/common';
import { Component, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RecruiterJobsService, RecruiterJobViewModel } from '../../services/recruiter-jobs.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';



@Component({
  selector: 'app-recruiter-job-list',
  imports: [CommonModule, RouterLink, JobCardComponent, TranslatePipe],
  templateUrl: './recruiter-job-list.component.html',
  styleUrl: './recruiter-job-list.component.css',
})
export class RecruiterJobListComponent {
  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(10);

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    effect(() => {
      const currentPageIndex = this.pageIndex();
      const currentPageSize = this.pageSize();
      this.recruiterJobsService.loadPostedJobs(currentPageIndex, currentPageSize);
    });

    this.recruiterJobsService.loadPostedJobCount();
  }

  get jobs(): RecruiterJobViewModel[] {
    return this.recruiterJobsService.postedJobs$();
  }

  mapJobToCard(job: RecruiterJobViewModel): JobCardModel {
    return {
      id: job.id,
      title: job.title,
      address: job.address,
      salary: job.salary,
      time: job.time
    };
  }

  get isLoading(): boolean {
    return this.recruiterJobsService.isLoadingPostedJobs$();
  }

  get totalPostedJobs(): number {
    return this.recruiterJobsService.postedJobsTotalCount$();
  }

  get canGoPrev(): boolean {
    return this.pageIndex() > 0;
  }

  get canGoNext(): boolean {
    return this.pageIndex() + 1 < Math.max(this.recruiterJobsService.postedJobsTotalPages$(), 1);
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

  deleteJob(jobId: string | number): void {
    const normalizedId = Number(jobId);
    if (!Number.isFinite(normalizedId) || normalizedId <= 0) {
      return;
    }

    this.recruiterJobsService.deletePostedJob(normalizedId).subscribe({
      next: (response) => {
        this.notify.success(
          response.message || this.i18nService.translate('recruiterJobList.notifications.deleteSuccess')
        );
        this.recruiterJobsService.loadPostedJobs(this.pageIndex(), this.pageSize());
        this.recruiterJobsService.loadPostedJobCount();
      },
      error: (error: { error?: { message?: string } }) => {
        this.notify.error(
          error?.error?.message || this.i18nService.translate('recruiterJobList.notifications.deleteFailed')
        );
      }
    });
  }
}
