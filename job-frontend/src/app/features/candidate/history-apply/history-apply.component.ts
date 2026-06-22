import { Component, OnInit, inject, effect } from '@angular/core';
import { JobCardComponent } from '../../../shared/components/job-card/job-card.component';
import { SkeletonCvCardComponent } from '../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../core/services/resume.service';
import { JobService } from '../../jobs/services/job.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { SkeletonJobCardComponent } from '../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import { JobCardModel } from '../../../shared/models/jobs/job-card.model';

@Component({
  selector: 'app-history-apply',
  standalone: true,
  imports: [JobCardComponent, LoadingComponent, SkeletonJobCardComponent],
  templateUrl: './history-apply.component.html',
  styleUrl: './history-apply.component.css'
})
export class HistoryApplyComponent implements OnInit {
  private readonly jobService = inject(JobService);
  skeleton = this.getSkeletonFlag();
  readonly skeletonRows = [1, 2, 3];
  private readonly defaultPageSize = 10;

  appliedJobs: JobCardModel[] = [];
  isLoading = false;
  hasMoreAppliedJobs = false;

  constructor() {
    effect(() => {
      this.appliedJobs = this.jobService.appliedJobs$();
      this.isLoading = this.jobService.isLoadingAppliedJobs$();
      this.hasMoreAppliedJobs = this.jobService.hasMoreAppliedJobs$();
    });
  }

  ngOnInit(): void {
    if (this.appliedJobs.length === 0) {
      this.jobService.loadMoreAppliedJobs(this.defaultPageSize);
    }
  }

  loadMoreAppliedJobs(): void {
    this.jobService.loadMoreAppliedJobs(this.defaultPageSize);
  }

  private getSkeletonFlag(): boolean {
    return true;
  }

  get totalAppliedJobs(): number {
    return this.appliedJobs.length;
  }

  get averageSalaryLabel(): string {
    const jobs = this.appliedJobs;
    if (jobs.length === 0) {
      return '--';
    }

    const totalSalary = jobs.reduce((sum, job) => sum + job.salary, 0);
    const averageSalary = totalSalary / jobs.length;
    return `${Math.round(averageSalary).toLocaleString('vi-VN')} VND`;
  }

  get highestSalaryLabel(): string {
    const jobs = this.appliedJobs;
    if (jobs.length === 0) {
      return '--';
    }

    const highestSalary = Math.max(...jobs.map(job => job.salary));
    return `${highestSalary.toLocaleString('vi-VN')} VND`;
  }

  get latestAppliedTime(): string {
    const jobs = this.appliedJobs;
    if (jobs.length === 0) {
      return '--';
    }

    return jobs[0].time;
  }
}
