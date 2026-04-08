import { CommonModule } from '@angular/common';
import { Component, effect, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RecruiterJobsService, RecruiterJobViewModel } from '../../services/recruiter-jobs.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { HirerJobCardComponent } from '../../components/hirer-job-card/hirer-job-card.component';



@Component({
  selector: 'app-recruiter-job-list',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe, HirerJobCardComponent],
  templateUrl: './recruiter-job-list.component.html',
  styleUrl: './recruiter-job-list.component.css',
})
export class RecruiterJobListComponent {
  private readonly pageIndex = signal(0);
  private readonly pageSize = signal(10);
  readonly pageSizeOptions = [5, 10, 20, 50];

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService,
    private readonly i18nService: I18nService
  ) {
    effect(() => {
      const currentPageIndex = this.pageIndex();
      const currentPageSize = this.pageSize();
      this.recruiterJobsService.loadPostedJobs(currentPageIndex, currentPageSize);
      this.recruiterJobsService.loadPostedJobCount();
    });

    effect(() => {
      const actionTick = this.recruiterJobsService.actionTick$();
      const actionType = this.recruiterJobsService.lastActionType$();
      if (actionTick === 0 || actionType !== 'delete') {
        return;
      }

      const isSuccess = this.recruiterJobsService.lastActionSuccess$();
      const message = this.recruiterJobsService.lastActionMessage$();

      if (isSuccess) {
        this.notify.success(message || this.i18nService.translate('recruiterJobList.notifications.deleteSuccess'));
        this.recruiterJobsService.loadPostedJobs(this.pageIndex(), this.pageSize());
        this.recruiterJobsService.loadPostedJobCount();
        return;
      }

      this.notify.error(message || this.i18nService.translate('recruiterJobList.notifications.deleteFailed'));
    });
  }

  get jobs(): RecruiterJobViewModel[] {
    return this.recruiterJobsService.postedJobs$();
  }

  get isLoading(): boolean {
    return this.recruiterJobsService.isLoadingPostedJobs$();
  }

  get totalPostedJobs(): number {
    return this.recruiterJobsService.postedJobsTotalCount$();
  }

  get currentPage(): number {
    return this.pageIndex() + 1;
  }

  get totalPages(): number {
    return Math.max(this.recruiterJobsService.postedJobsTotalPages$(), 1);
  }

  get currentPageSize(): number {
    return this.pageSize();
  }

  get pageButtons(): number[] {
    const total = this.totalPages;
    const current = this.currentPage;
    const start = Math.max(1, current - 2);
    const end = Math.min(total, start + 4);
    const adjustedStart = Math.max(1, end - 4);

    const pages: number[] = [];
    for (let page = adjustedStart; page <= end; page += 1) {
      pages.push(page);
    }
    return pages;
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

  goToPage(page: number): void {
    if (!Number.isFinite(page)) {
      return;
    }

    const normalizedPage = Math.floor(page);
    if (normalizedPage < 1 || normalizedPage > this.totalPages || normalizedPage === this.currentPage) {
      return;
    }

    this.pageIndex.set(normalizedPage - 1);
  }

  onPageSizeChange(event: Event): void {
    const selectedSize = Number((event.target as HTMLSelectElement).value);
    if (!Number.isFinite(selectedSize) || selectedSize <= 0 || selectedSize === this.pageSize()) {
      return;
    }

    this.pageSize.set(selectedSize);
    this.pageIndex.set(0);
  }

  deleteJob(jobId: string | number): void {
    const normalizedId = Number(jobId);
    if (!Number.isFinite(normalizedId) || normalizedId <= 0) {
      return;
    }

    this.recruiterJobsService.deletePostedJob(normalizedId);
  }
}
