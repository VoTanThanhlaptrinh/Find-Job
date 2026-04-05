import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

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
  readonly metrics: DashboardMetric[] = [
    {
      titleKey: 'recruiterOverview.metrics.openJobsTitle',
      value: '12',
      trendKey: 'recruiterOverview.metrics.openJobsTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.newCandidatesTitle',
      value: '34',
      trendKey: 'recruiterOverview.metrics.newCandidatesTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.interviewsTitle',
      value: '9',
      trendKey: 'recruiterOverview.metrics.interviewsTrend',
    },
    {
      titleKey: 'recruiterOverview.metrics.responseRateTitle',
      value: '86%',
      trendKey: 'recruiterOverview.metrics.responseRateTrend',
    },
  ];
}
