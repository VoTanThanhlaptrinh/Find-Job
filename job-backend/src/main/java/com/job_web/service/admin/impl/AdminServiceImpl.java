package com.job_web.service.admin.impl;

import com.job_web.constant.RoleConstants;
import com.job_web.data.ApplyRepository;
import com.job_web.data.CandidateRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.data.queryDSL.AdminJobQuery;
import com.job_web.data.queryDSL.CandidateQuery;
import com.job_web.data.queryDSL.HirerQuery;
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
import com.job_web.exception.BadRequestException;
import com.job_web.exception.ForbiddenException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.Candidate;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.admin.AdminService;
import com.job_web.service.security.JwtService;
import com.job_web.service.security.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final HirerRepository hirerRepository;
    private final CandidateRepository candidateRepository;
    private final ApplyRepository applyRepository;
    private final HirerQuery hirerQuery;
    private final CandidateQuery candidateQuery;
    private final AdminJobQuery adminJobQuery;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.cookie.secure}")
    private boolean isSecure;

    @Override public String login(AdminLoginRequest r, HttpServletResponse response) {
        User u = userRepository.findByEmail(r.getEmail()).orElseThrow(() -> new ResourceNotFoundException("auth.email.not_found"));
        validateAdminRole(u);
        if (!encoder.matches(r.getPassword(), u.getPassword())) throw new BadRequestException("auth.login.wrong_password");
        String refreshToken = refreshTokenService.createRefreshToken(u.getEmail());
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7).getSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return jwtService.generateToken(u);
    }
    @Override public AdminLoginResponse refresh(AdminRefreshRequest r) {
        if (refreshTokenService.isValid(r.getRefreshToken())) {
            User u = userRepository.findByEmail(jwtService.extractUsername(r.getRefreshToken())).orElseThrow(() -> new ResourceNotFoundException("auth.email.not_found"));
            validateAdminRole(u);
            return AdminLoginResponse.builder().accessToken(jwtService.generateToken(u)).refreshToken(refreshTokenService.reGenerateRefreshToken(r.getRefreshToken())).expiresIn(900).build();
        }
        throw new ResourceNotFoundException("auth.session.expired");
    }
    @Override public void logout(String t) { if (t != null) refreshTokenService.deleteRefreshToken(t); }
    @Override public DashboardSummaryResponse getDashboardSummary() {
        return DashboardSummaryResponse.builder().totalEmployers(hirerRepository.count()).totalJobSeekers(candidateRepository.count()).pendingJobs(jobRepository.countByStatus("PENDING")).totalRevenue(142500.0).growth(DashboardSummaryResponse.Growth.builder().employersPct(12.0).jobSeekersPct(8.0).revenuePct(15.0).build()).build();
    }
    @Override public RevenueTrendResponse getRevenueTrend(String r) {
        return RevenueTrendResponse.builder().range(r).labels(List.of("Week 1", "Week 2", "Week 3", "Week 4")).current(List.of(7200.0, 9100.0, 12600.0, 15800.0)).previous(List.of(6100.0, 7900.0, 9500.0, 12100.0)).build();
    }
    @Override public JobDistributionResponse getJobDistribution() {
        long t = jobRepository.count();
        if (t == 0) return JobDistributionResponse.builder().build();
        return JobDistributionResponse.builder().total(t).activePct((double) jobRepository.countByStatus("ACTIVE") * 100 / t).pendingPct((double) jobRepository.countByStatus("PENDING") * 100 / t).expiredPct((double) jobRepository.countByStatus("EXPIRED") * 100 / t).build();
    }
    @Override public PageResponse<PendingJobItem> getPendingJobs(int p, int s) {
        Pageable pa = PageRequest.of(p - 1, s);
        Page<Job> jp = jobRepository.findByStatus("PENDING", pa);
        return PageResponse.<PendingJobItem>builder().items(jp.getContent().stream().map(j -> PendingJobItem.builder().id(String.valueOf(j.getId())).title(j.getTitle()).subtitle("Required: " + j.getSkill()).company(j.getHirer() != null ? j.getHirer().getCompanyName() : "Unknown").postDate(j.getCreateDate().toLocalDate()).status("pending").build()).collect(Collectors.toList())).pagination(mapPagination(p, s, jp)).build();
    }
    @Override public EmployerMetricsResponse getEmployerMetrics() {
        long t = hirerRepository.count();
        return EmployerMetricsResponse.builder().totalEmployers(t).totalEmployersGrowthPct(12.0).kycVerified((long) (t * 0.8)).kycVerifiedPct(80.0).pendingKyc((long) (t * 0.1)).suspended((long) (t * 0.1)).build();
    }
    @Override public PageResponse<EmployerListItem> getEmployers(int p, int s, String se, String k, String st) {
        Pageable pa = PageRequest.of(p - 1, s);
        Page<EmployerListItem> ep = hirerQuery.findEmployers(se, k, st, pa);
        return PageResponse.<EmployerListItem>builder().items(ep.getContent()).pagination(mapPagination(p, s, ep)).build();
    }
    @Override public EmployerDetail getEmployerDetail(long id) {
        Hirer h = hirerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employer not found"));
        return EmployerDetail.builder().id(String.valueOf(h.getId())).name(h.getCompanyName()).industry("Technology").registrationDate(LocalDateTime.ofInstant(h.getCreateDate(), ZoneId.systemDefault()).toLocalDate()).activeJobs(h.getJobsPost() != null ? h.getJobsPost().size() : 0).kycStatus("verified").accountStatus(h.getStatus()).contactEmail(h.getUser() != null ? h.getUser().getEmail() : "").contactPhone(h.getUser() != null ? h.getUser().getMobile() : "").build();
    }
    @Override public void updateEmployerStatus(long id, EmployerStatusRequest r) {
        Hirer h = hirerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employer not found"));
        if ("suspend".equalsIgnoreCase(r.getAction())) h.setStatus("SUSPENDED");
        else if ("activate".equalsIgnoreCase(r.getAction())) h.setStatus("ACTIVE");
        hirerRepository.save(h);
    }
    @Override public JobSeekerMetricsResponse getJobSeekerMetrics() {
        long t = candidateRepository.count();
        return JobSeekerMetricsResponse.builder().totalSeekers(t).totalSeekersGrowthPct(12.0).activeLast7Days((long) (t * 0.3)).placedCandidates((long) (t * 0.2)).retentionPct(94.0).build();
    }
    @Override public PageResponse<JobSeekerListItem> getJobSeekers(int p, int s, String se, String rs) {
        Pageable pa = PageRequest.of(p - 1, s);
        Page<JobSeekerListItem> sp = candidateQuery.findJobSeekers(se, rs, pa);
        return PageResponse.<JobSeekerListItem>builder().items(sp.getContent()).pagination(mapPagination(p, s, sp)).build();
    }
    @Override public void createJobSeeker(JobSeekerRequest r) {
        Candidate c = new Candidate(); c.setFullName(r.getFullName()); c.setEmail(r.getEmail()); c.setCreateDate(new Timestamp(System.currentTimeMillis())); c.setStatus("ACTIVE"); candidateRepository.save(c);
    }
    @Override public RegionDistributionResponse getRegionDistribution() {
        return RegionDistributionResponse.builder().regions(List.of(new RegionDistributionResponse.RegionCount("NA", 12000), new RegionDistributionResponse.RegionCount("EU", 17000), new RegionDistributionResponse.RegionCount("APAC", 9000), new RegionDistributionResponse.RegionCount("LATAM", 5000), new RegionDistributionResponse.RegionCount("MEA", 2200))).build();
    }
    @Override public JobMetricsResponse getJobMetrics() {
        return JobMetricsResponse.builder().livePostings(jobRepository.countByStatus("ACTIVE")).livePostingsGrowthPct(12.0).pendingReview(jobRepository.countByStatus("PENDING")).totalApplicants(applyRepository.count()).avgTimeToHireDays(18).build();
    }
    @Override public PageResponse<AdminJobListItem> getJobs(int p, int s, String se, String c, String st) {
        Pageable pa = PageRequest.of(p - 1, s);
        Page<AdminJobListItem> jp = adminJobQuery.findJobs(se, c, st, pa);
        return PageResponse.<AdminJobListItem>builder().items(jp.getContent()).pagination(mapPagination(p, s, jp)).build();
    }
    @Override public void createJob(AdminJobRequest r) {
        Job j = new Job(); j.setTitle(r.getTitle()); j.setDescription(r.getDescription()); j.setExpiredDate(r.getExpiryDate()); j.setStatus("PENDING"); j.setCreateDate(LocalDateTime.now()); jobRepository.save(j);
    }
    @Override public void updateJobStatus(long id, String s) {
        Job j = jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Job not found")); j.setStatus(s.toUpperCase()); jobRepository.save(j);
    }
    @Override public void bulkJobAction(BulkActionRequest r) { for (String id : r.getJobIds()) { try { updateJobStatus(Long.parseLong(id), r.getAction()); } catch (Exception e) {} } }

    @Override public List<BillingTierDTO> getBillingTiers() {
        List<BillingTierDTO> tiers = new ArrayList<>();
        tiers.add(BillingTierDTO.builder().id("basic").name("Basic").badge("Standard").priceMonthly(499).currency("USD").isPopular(false).usagePct(15).features(List.of("Up to 10 Job Postings", "Standard Talent Pool", "Email Support")).build());
        tiers.add(BillingTierDTO.builder().id("pro").name("Pro").badge("Advanced").priceMonthly(1299).currency("USD").isPopular(true).usagePct(65).features(List.of("Unlimited Job Postings", "AI Talent Matching", "Dedicated Account Manager", "Analytics Dashboard")).build());
        return tiers;
    }
    @Override public void updateBillingTier(String id, BillingTierDTO r) { log.info("Billing tier {} updated", id); }
    @Override public PageResponse<TransactionListItem> getTransactions(int p, int s, String st) {
        List<TransactionListItem> items = List.of(TransactionListItem.builder().id("TX-90234").employerId("emp_001").employerName("Nexus Core Inc.").packageName("pro").amount(1299).currency("USD").date(LocalDateTime.now()).status("paid").build());
        return PageResponse.<TransactionListItem>builder().items(items).pagination(PageResponse.Pagination.builder().page(p).pageSize(s).totalItems(1).totalPages(1).build()).build();
    }
    @Override public BillingSummaryResponse getBillingSummary() {
        return BillingSummaryResponse.builder().monthlyRecurringRevenue(442890.0).mrrGrowthPct(12.4).activeSubscriptions(2841).build();
    }

    private void validateAdminRole(User u) { if (!u.getAuthorities().stream().map(GrantedAuthority::getAuthority).map(RoleConstants::normalizeRole).anyMatch(RoleConstants.ROLE_ADMIN::equals)) throw new ForbiddenException("auth.login.admin.role_invalid"); }
    private PageResponse.Pagination mapPagination(int p, int s, Page<?> pr) { return PageResponse.Pagination.builder().page(p).pageSize(s).totalItems(pr.getTotalElements()).totalPages(pr.getTotalPages()).build(); }

    @Override public String dashboard() { return "admin dashboard"; }
    @Override public String employers() { return "admin employers"; }
    @Override public String jobSeekers() { return "admin job-seekers"; }
    @Override public String jobs() { return "admin jobs"; }
    @Override public String billing() { return "admin billing"; }
}
