package com.job_web.service.admin.impl;

import com.job_web.dto.admin.billing.BillingSummaryResponse;
import com.job_web.dto.admin.billing.BillingTierDTO;
import com.job_web.dto.admin.billing.TransactionListItem;
import com.job_web.dto.admin.dashboard.DashboardSummaryResponse;
import com.job_web.dto.admin.dashboard.JobDistributionResponse;
import com.job_web.dto.admin.dashboard.PendingJobItem;
import com.job_web.dto.admin.dashboard.RevenueTrendResponse;
import com.job_web.dto.admin.employer.EmployerDetail;
import com.job_web.dto.admin.employer.EmployerListItem;
import com.job_web.dto.admin.employer.EmployerMetricsResponse;
import com.job_web.dto.admin.employer.EmployerStatusRequest;
import com.job_web.dto.admin.job.AdminJobListItem;
import com.job_web.dto.admin.job.AdminJobRequest;
import com.job_web.dto.admin.job.BulkActionRequest;
import com.job_web.dto.admin.job.JobMetricsResponse;
import com.job_web.dto.admin.seeker.JobSeekerListItem;
import com.job_web.dto.admin.seeker.JobSeekerMetricsResponse;
import com.job_web.dto.admin.seeker.JobSeekerRequest;
import com.job_web.dto.admin.seeker.RegionDistributionResponse;
import com.job_web.dto.common.PageResponse;
import com.job_web.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminBillingServiceImpl billingService;
    private final AdminDashboardServiceImpl dashboardService;
    private final AdminEmployerServiceImpl employerService;
    private final AdminJobSeekerServiceImpl jobSeekerService;
    private final AdminJobServiceImpl jobService;
    private final AdminNavigationServiceImpl navigationService;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        return dashboardService.getDashboardSummary();
    }

    @Override
    public RevenueTrendResponse getRevenueTrend(String range) {
        return dashboardService.getRevenueTrend(range);
    }

    @Override
    public JobDistributionResponse getJobDistribution() {
        return dashboardService.getJobDistribution();
    }

    @Override
    public PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize) {
        return dashboardService.getPendingJobs(page, pageSize);
    }

    @Override
    public EmployerMetricsResponse getEmployerMetrics() {
        return employerService.getEmployerMetrics();
    }

    @Override
    public PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status) {
        return employerService.getEmployers(page, pageSize, search, kycStatus, status);
    }

    @Override
    public EmployerDetail getEmployerDetail(long id) {
        return employerService.getEmployerDetail(id);
    }

    @Override
    public void updateEmployerStatus(long id, EmployerStatusRequest request) {
        employerService.updateEmployerStatus(id, request);
    }

    @Override
    public JobSeekerMetricsResponse getJobSeekerMetrics() {
        return jobSeekerService.getJobSeekerMetrics();
    }

    @Override
    public PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus) {
        return jobSeekerService.getJobSeekers(page, pageSize, search, resumeStatus);
    }

    @Override
    public void createJobSeeker(JobSeekerRequest request) {
        jobSeekerService.createJobSeeker(request);
    }

    @Override
    public RegionDistributionResponse getRegionDistribution() {
        return jobSeekerService.getRegionDistribution();
    }

    @Override
    public JobMetricsResponse getJobMetrics() {
        return jobService.getJobMetrics();
    }

    @Override
    public PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status) {
        return jobService.getJobs(page, pageSize, search, category, status);
    }

    @Override
    public void createJob(AdminJobRequest request) {
        jobService.createJob(request);
    }

    @Override
    public void updateJobStatus(long id, String status) {
        jobService.updateJobStatus(id, status);
    }

    @Override
    public void bulkJobAction(BulkActionRequest request) {
        jobService.bulkJobAction(request);
    }

    @Override
    public List<BillingTierDTO> getBillingTiers() {
        return billingService.getBillingTiers();
    }

    @Override
    public void updateBillingTier(String id, BillingTierDTO request) {
        billingService.updateBillingTier(id, request);
    }

    @Override
    public PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status) {
        return billingService.getTransactions(page, pageSize, status);
    }

    @Override
    public BillingSummaryResponse getBillingSummary() {
        return billingService.getBillingSummary();
    }

    @Override
    public String dashboard() {
        return navigationService.dashboard();
    }

    @Override
    public String employers() {
        return navigationService.employers();
    }

    @Override
    public String jobSeekers() {
        return navigationService.jobSeekers();
    }

    @Override
    public String jobs() {
        return navigationService.jobs();
    }

    @Override
    public String billing() {
        return navigationService.billing();
    }
}
