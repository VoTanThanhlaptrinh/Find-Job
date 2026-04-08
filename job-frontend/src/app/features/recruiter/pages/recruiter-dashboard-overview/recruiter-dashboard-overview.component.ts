import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';

type DashboardMetric = {
  titleKey: string;
  value: string;
  trendKey: string;
};

@Component({
  selector: 'app-recruiter-dashboard-overview',
  imports: [CommonModule, TranslatePipe],
  templateUrl: './recruiter-dashboard-overview.component.html',
  styleUrl: './recruiter-dashboard-overview.component.css',
})
export class RecruiterDashboardOverviewComponent {
  private readonly recruiterJobsService = inject(RecruiterJobsService);
  private readonly openJobsCount = signal<number | null>(null);

  readonly metrics = computed<DashboardMetric[]>(() => [
    {
      titleKey: 'recruiterOverview.metrics.openJobsTitle',
      value: this.openJobsCount() === null ? 'Khong co du lieu' : String(this.openJobsCount()),
      trendKey: 'recruiterOverview.metrics.openJobsTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.newCandidatesTitle',
      value: 'Khong co du lieu',
      trendKey: 'recruiterOverview.metrics.newCandidatesTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.interviewsTitle',
      value: 'Khong co du lieu',
      trendKey: 'recruiterOverview.metrics.interviewsTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.responseRateTitle',
      value: 'Khong co du lieu',
      trendKey: 'recruiterOverview.metrics.responseRateTrend',
    },
  ]);

  constructor() {
    this.recruiterJobsService.loadPostedJobCount();

    effect(() => {
      this.openJobsCount.set(this.recruiterJobsService.postedJobsTotalCount$());
    });
  }
}
