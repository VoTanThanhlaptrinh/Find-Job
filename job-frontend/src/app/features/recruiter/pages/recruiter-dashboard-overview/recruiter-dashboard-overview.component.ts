import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnInit } from '@angular/core';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';

type DashboardMetric = {
  titleKey: string;
  value: string;
  trendKey: string;
};

@Component({
  selector: 'app-recruiter-dashboard-overview',
  imports: [CommonModule],
  templateUrl: './recruiter-dashboard-overview.component.html',
  styleUrl: './recruiter-dashboard-overview.component.css',
})
export class RecruiterDashboardOverviewComponent implements OnInit {
  private readonly recruiterJobsService = inject(RecruiterJobsService);
  
  openJobsCount: number | null = null;
  metrics: DashboardMetric[] = [];

  constructor() {
    effect(() => {
      this.openJobsCount = this.recruiterJobsService.postedJobsTotalCount$();
      
      this.metrics = [
        {
          titleKey: 'recruiterOverview.metrics.openJobsTitle',
          value: this.openJobsCount === null ? 'Khong co du lieu' : String(this.openJobsCount),
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
      ];
    });
  }

  ngOnInit(): void {
    this.recruiterJobsService.loadPostedJobCount();
  }
}
