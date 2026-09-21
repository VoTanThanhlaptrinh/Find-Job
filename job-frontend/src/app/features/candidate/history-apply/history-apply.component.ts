import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { JobCardComponent } from '../../../shared/components/job-card/job-card.component';
import { JobService } from '../../jobs/services/job.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { SkeletonJobCardComponent } from '../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import { JobCardModel } from '../../../shared/models/jobs/job-card.model';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-history-apply',
  standalone: true,
  imports: [CommonModule, RouterLink, JobCardComponent, LoadingComponent, SkeletonJobCardComponent, TranslatePipe],
  templateUrl: './history-apply.component.html',
  styleUrl: './history-apply.component.css'
})
export class HistoryApplyComponent implements OnInit {
  private readonly jobService = inject(JobService);
  private readonly i18n = inject(I18nService);

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

  get mostCommonLocation(): string {
    const jobs = this.appliedJobs;
    if (jobs.length === 0) return '--';
    const defaultNationwide = this.i18n.translate('category.card.nationwide');
    const counts = jobs.reduce((acc, job) => {
      const loc = job.address || defaultNationwide;
      acc[loc] = (acc[loc] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
    return Object.keys(counts).reduce((a, b) => counts[a] > counts[b] ? a : b);
  }

  get mostCommonJobType(): string {
    const jobs = this.appliedJobs;
    if (jobs.length === 0) return '--';
    const counts = jobs.reduce((acc, job) => {
      const type = job.time || '';
      acc[type] = (acc[type] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);
    const rawType = Object.keys(counts).reduce((a, b) => counts[a] > counts[b] ? a : b);
    if (!rawType || rawType === '--') return '--';
    const upper = String(rawType).toUpperCase();
    const key = `category.card.employmentTypes.${upper}`;
    const translated = this.i18n.translate(key);
    return translated !== key ? translated : rawType;
  }
}
