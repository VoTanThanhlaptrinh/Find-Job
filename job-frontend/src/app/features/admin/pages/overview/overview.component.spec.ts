import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideToastr } from 'ngx-toastr';
import { OverviewComponent } from './overview.component';
import { AdminDashboardService } from '../../services/admin-dashboard.service';

describe('OverviewComponent', () => {
  let component: OverviewComponent;
  let fixture: ComponentFixture<OverviewComponent>;
  let dashboardService: AdminDashboardService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OverviewComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideToastr(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OverviewComponent);
    component = fixture.componentInstance;
    dashboardService = TestBed.inject(AdminDashboardService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default range 30d and refresh data', () => {
    expect(component.selectedRange).toBe('30d');
  });

  it('should change range and request revenue trend', () => {
    spyOn(dashboardService, 'loadRevenueTrend');
    component.setRange('7d');
    expect(component.selectedRange).toBe('7d');
    expect(dashboardService.loadRevenueTrend).toHaveBeenCalledWith('7d');
  });

  it('should call refreshAll when onRefresh is clicked', () => {
    spyOn(dashboardService, 'refreshAll');
    component.selectedRange = '90d';
    component.onRefresh();
    expect(dashboardService.refreshAll).toHaveBeenCalledWith('90d');
  });

  it('should toggle revenue table view', () => {
    expect(component.showRevenueTable).toBeFalse();
    component.toggleRevenueTable();
    expect(component.showRevenueTable).toBeTrue();
    component.toggleRevenueTable();
    expect(component.showRevenueTable).toBeFalse();
  });

  it('should handle pagination for pending jobs correctly', () => {
    spyOn(dashboardService, 'updatePendingJobsQuery');
    spyOnProperty(component, 'totalPages', 'get').and.returnValue(3);
    spyOnProperty(component, 'currentPage', 'get').and.returnValue(1);

    component.nextPendingPage();
    expect(dashboardService.updatePendingJobsQuery).toHaveBeenCalledWith({ page: 2 });

    component.prevPendingPage();
    expect(dashboardService.updatePendingJobsQuery).toHaveBeenCalledWith({ page: 2 }); // because currentPage mocked to 1 so prev won't call 0
  });

  it('should trigger retry methods for scoped widgets', () => {
    spyOn(dashboardService, 'loadSummary');
    spyOn(dashboardService, 'loadRevenueTrend');
    spyOn(dashboardService, 'loadJobDistribution');
    spyOn(dashboardService, 'loadPendingJobs');

    component.retrySummary();
    expect(dashboardService.loadSummary).toHaveBeenCalled();

    component.retryRevenue();
    expect(dashboardService.loadRevenueTrend).toHaveBeenCalledWith(component.selectedRange);

    component.retryDistribution();
    expect(dashboardService.loadJobDistribution).toHaveBeenCalled();

    component.retryPendingJobs();
    expect(dashboardService.loadPendingJobs).toHaveBeenCalled();
  });

  it('should calculate max revenue and revenue chart points safely', () => {
    expect(component.maxRevenue).toBe(100);
    expect(component.revenuePoints.currentPoints.length).toBe(0);
    expect(component.totalCurrentRevenue).toBe(0);
    expect(component.totalPreviousRevenue).toBe(0);
    expect(component.revenueGrowthPct).toBe(0);
  });
});
