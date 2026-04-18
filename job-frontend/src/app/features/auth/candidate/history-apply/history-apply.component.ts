import { Component, effect, inject } from '@angular/core';
import { JobService } from '../../../jobs/services/job.service';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { LoadingComponent } from '../../../../shared/components/loading/loading.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';

@Component({
  selector: 'app-history-apply',
  standalone: true,
  imports: [JobCardComponent, LoadingComponent, SkeletonJobCardComponent],
  templateUrl: './history-apply.component.html',
  styleUrl: './history-apply.component.css'
})
export class HistoryApplyComponent {
  private readonly jobService = inject(JobService);
  skeleton = this.getSkeletonFlag();
  readonly skeletonRows = [1, 2, 3];
  private readonly defaultPageSize = 10;
  private hasRequestedInitialData = false;

  readonly appliedJobs = this.jobService.appliedJobs$;
  readonly isLoading = this.jobService.isLoadingAppliedJobs$;
  readonly hasMoreAppliedJobs = this.jobService.hasMoreAppliedJobs$;

  constructor() {
    effect(() => {
      const jobs = this.appliedJobs();
      const loading = this.isLoading();

      if (!this.hasRequestedInitialData && jobs.length === 0 && !loading) {
        this.hasRequestedInitialData = true;
        this.jobService.loadMoreAppliedJobs(this.defaultPageSize);
      }
    });
  }

  loadMoreAppliedJobs(): void {
    this.jobService.loadMoreAppliedJobs(this.defaultPageSize);
  }

  private getSkeletonFlag(): boolean {
    return true;
  }

  get totalAppliedJobs(): number {
    return this.appliedJobs().length;
  }

  get averageSalaryLabel(): string {
    const jobs = this.appliedJobs();
    if (jobs.length === 0) {
      return '--';
    }

    const totalSalary = jobs.reduce((sum, job) => sum + job.salary, 0);
    const averageSalary = totalSalary / jobs.length;
    return `${Math.round(averageSalary).toLocaleString('vi-VN')} VND`;
  }

  get highestSalaryLabel(): string {
    const jobs = this.appliedJobs();
    if (jobs.length === 0) {
      return '--';
    }

    const highestSalary = Math.max(...jobs.map(job => job.salary));
    return `${highestSalary.toLocaleString('vi-VN')} VND`;
  }

  get latestAppliedTime(): string {
    const jobs = this.appliedJobs();
    if (jobs.length === 0) {
      return '--';
    }

    return jobs[0].time;
  }
}
