package com.job_web.service.admin;

import com.job_web.dto.admin.auth.AdminLoginRequest;
import com.job_web.dto.admin.auth.AdminLoginResponse;
import com.job_web.dto.admin.auth.AdminRefreshRequest;
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
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AdminService {
    String login(AdminLoginRequest request, HttpServletResponse response);

    AdminLoginResponse refresh(AdminRefreshRequest request);

    void logout(String refreshToken);

    DashboardSummaryResponse getDashboardSummary();

    RevenueTrendResponse getRevenueTrend(String range);

    JobDistributionResponse getJobDistribution();

    PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize);

    EmployerMetricsResponse getEmployerMetrics();

    PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status);

    EmployerDetail getEmployerDetail(long id);

    void updateEmployerStatus(long id, EmployerStatusRequest request);

    JobSeekerMetricsResponse getJobSeekerMetrics();

    PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus);

    void createJobSeeker(JobSeekerRequest request);

    RegionDistributionResponse getRegionDistribution();

    JobMetricsResponse getJobMetrics();

    PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status);

    void createJob(AdminJobRequest request);

    void updateJobStatus(long id, String status);

    void bulkJobAction(BulkActionRequest request);

    List<BillingTierDTO> getBillingTiers();

    void updateBillingTier(String id, BillingTierDTO request);

    PageResponse<TransactionListItem> getTransactions(int page, int pageSize, String status);

    BillingSummaryResponse getBillingSummary();

    String dashboard();

    String employers();

    String jobSeekers();

    String jobs();

    String billing();
}
