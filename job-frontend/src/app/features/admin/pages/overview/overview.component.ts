import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminDashboardService } from '../../services/admin-dashboard.service';

export type DashboardRange = '7d' | '30d' | '90d';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css',
})
export class OverviewComponent implements OnInit {
  private readonly dashboardService = inject(AdminDashboardService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  selectedRange: DashboardRange = '30d';
  showRevenueTable: boolean = false;

  readonly rangeOptions: Array<{ label: string; value: DashboardRange }> = [
    { label: '7 ngày', value: '7d' },
    { label: '30 ngày', value: '30d' },
    { label: '90 ngày', value: '90d' },
  ];

  ngOnInit(): void {
    const q = this.route.snapshot.queryParams;
    if (q['range'] === '7d' || q['range'] === '30d' || q['range'] === '90d') {
      this.selectedRange = q['range'];
    }
    const initialPage = Number(q['page']);
    if (initialPage > 1) {
      this.dashboardService.updatePendingJobsQuery({ page: initialPage });
    }

    this.dashboardService.refreshAll(this.selectedRange);
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

  get pendingJobsQuery() {
    return this.dashboardService.pendingJobsQuery();
  }

  get currentPage(): number {
    return this.pendingJobsQuery.page || 1;
  }

  get pageSize(): number {
    return this.pendingJobsQuery.pageSize || 10;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.pendingJobsTotal / this.pageSize));
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

  get isAnyLoading(): boolean {
    return (
      this.isLoadingSummary ||
      this.isLoadingRevenue ||
      this.isLoadingDistribution ||
      this.isLoadingPendingJobs
    );
  }

  get errorSummary(): string | null {
    return this.dashboardService.errorSummary();
  }

  get errorRevenue(): string | null {
    return this.dashboardService.errorRevenue();
  }

  get errorDistribution(): string | null {
    return this.dashboardService.errorDistribution();
  }

  get errorPendingJobs(): string | null {
    return this.dashboardService.errorPendingJobs();
  }

  setRange(range: DashboardRange): void {
    if (this.selectedRange === range && !this.errorRevenue) return;
    this.selectedRange = range;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { range },
      queryParamsHandling: 'merge',
    });
    this.dashboardService.loadRevenueTrend(range);
  }

  onRefresh(): void {
    this.dashboardService.refreshAll(this.selectedRange);
  }

  toggleRevenueTable(): void {
    this.showRevenueTable = !this.showRevenueTable;
  }

  retrySummary(): void {
    this.dashboardService.loadSummary();
  }

  retryRevenue(): void {
    this.dashboardService.loadRevenueTrend(this.selectedRange);
  }

  retryDistribution(): void {
    this.dashboardService.loadJobDistribution();
  }

  retryPendingJobs(): void {
    this.dashboardService.loadPendingJobs();
  }

  goToPendingPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) return;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
    this.dashboardService.updatePendingJobsQuery({ page });
  }

  prevPendingPage(): void {
    this.goToPendingPage(this.currentPage - 1);
  }

  nextPendingPage(): void {
    this.goToPendingPage(this.currentPage + 1);
  }

  trackByPendingJob(_: number, item: { id: string }): string {
    return item.id;
  }

  // --- SVG Chart Calculations ---
  get maxRevenue(): number {
    const trend = this.revenueTrend;
    if (!trend) return 100;
    const currentMax = trend.current?.length ? Math.max(...trend.current) : 0;
    const prevMax = trend.previous?.length ? Math.max(...trend.previous) : 0;
    const m = Math.max(currentMax, prevMax);
    return m > 0 ? m : 100;
  }

  get revenuePoints(): {
    currentLine: string;
    currentArea: string;
    previousLine: string;
    currentPoints: Array<{ x: number; y: number; val: number; label: string }>;
    previousPoints: Array<{ x: number; y: number; val: number; label: string }>;
  } {
    const trend = this.revenueTrend;
    if (!trend || !trend.labels?.length) {
      return {
        currentLine: '',
        currentArea: '',
        previousLine: '',
        currentPoints: [],
        previousPoints: [],
      };
    }
    const n = trend.labels.length;
    const width = 580;
    const height = 180;
    const padX = 40;
    const padY = 20;
    const chartW = width - padX * 2;
    const chartH = height - padY * 2;
    const max = this.maxRevenue;

    const currentCoords: Array<{ x: number; y: number; val: number; label: string }> = [];
    const prevCoords: Array<{ x: number; y: number; val: number; label: string }> = [];

    for (let i = 0; i < n; i++) {
      const x = n === 1 ? width / 2 : padX + (i / (n - 1)) * chartW;
      const curVal = trend.current?.[i] ?? 0;
      const prevVal = trend.previous?.[i] ?? 0;
      const yCur = padY + chartH - (curVal / max) * chartH;
      const yPrev = padY + chartH - (prevVal / max) * chartH;
      currentCoords.push({ x, y: yCur, val: curVal, label: trend.labels[i] });
      prevCoords.push({ x, y: yPrev, val: prevVal, label: trend.labels[i] });
    }

    const currentLine = currentCoords.map((p) => `${p.x},${p.y}`).join(' ');
    const previousLine = prevCoords.map((p) => `${p.x},${p.y}`).join(' ');

    const firstX = currentCoords[0]?.x ?? padX;
    const lastX = currentCoords[currentCoords.length - 1]?.x ?? padX + chartW;
    const bottomY = padY + chartH;
    const currentArea = `${firstX},${bottomY} ${currentLine} ${lastX},${bottomY}`;

    return {
      currentLine,
      currentArea,
      previousLine,
      currentPoints: currentCoords,
      previousPoints: prevCoords,
    };
  }

  get totalCurrentRevenue(): number {
    return this.revenueTrend?.current?.reduce((acc, val) => acc + val, 0) ?? 0;
  }

  get totalPreviousRevenue(): number {
    return this.revenueTrend?.previous?.reduce((acc, val) => acc + val, 0) ?? 0;
  }

  get revenueGrowthPct(): number {
    if (this.totalPreviousRevenue === 0) return 0;
    return Math.round(
      ((this.totalCurrentRevenue - this.totalPreviousRevenue) / this.totalPreviousRevenue) * 100
    );
  }

  // --- Donut Chart Calculations ---
  readonly donutRadius = 54;
  readonly donutCircumference = 2 * Math.PI * 54; // ~339.292

  get donutActiveDash(): string {
    const pct = this.jobDistribution?.activePct ?? 0;
    const len = (pct / 100) * this.donutCircumference;
    return `${len} ${this.donutCircumference}`;
  }

  get donutPendingDash(): string {
    const pct = this.jobDistribution?.pendingPct ?? 0;
    const len = (pct / 100) * this.donutCircumference;
    return `${len} ${this.donutCircumference}`;
  }

  get donutExpiredDash(): string {
    const pct = this.jobDistribution?.expiredPct ?? 0;
    const len = (pct / 100) * this.donutCircumference;
    return `${len} ${this.donutCircumference}`;
  }

  get donutPendingOffset(): number {
    const actPct = this.jobDistribution?.activePct ?? 0;
    return -((actPct / 100) * this.donutCircumference);
  }

  get donutExpiredOffset(): number {
    const actPct = this.jobDistribution?.activePct ?? 0;
    const pndPct = this.jobDistribution?.pendingPct ?? 0;
    return -(((actPct + pndPct) / 100) * this.donutCircumference);
  }
}

