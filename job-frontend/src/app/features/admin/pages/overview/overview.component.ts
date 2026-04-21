import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AdminDashboardService } from '../../services/admin-dashboard.service';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css',
})
export class OverviewComponent implements OnInit {
  private readonly dashboardService = inject(AdminDashboardService);

  ngOnInit(): void {
    this.dashboardService.refreshAll();
  }

  get summary() {
    return this.dashboardService.summary();
  }

  get revenueTrend() {
    return this.dashboardService.revenueTrend();
  }

  get jobDistribution() {
    return this.dashboardService.jobDistribution();
  }

  get pendingJobs() {
    return this.dashboardService.pendingJobs();
  }

  get pendingJobsTotal(): number {
    return this.dashboardService.pendingJobsTotal();
  }

  get isLoadingSummary(): boolean {
    return this.dashboardService.isLoadingSummary();
  }

  get isLoadingRevenue(): boolean {
    return this.dashboardService.isLoadingRevenue();
  }

  get isLoadingDistribution(): boolean {
    return this.dashboardService.isLoadingDistribution();
  }

  get isLoadingPendingJobs(): boolean {
    return this.dashboardService.isLoadingPendingJobs();
  }

  trackByPendingJob(_: number, item: { id: string }): string {
    return item.id;
  }

}
