export type AdminSortDirection = 'asc' | 'desc';

export interface AdminPagination {
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

export interface AdminListPayload<T> {
  items: T[];
  pagination: AdminPagination;
}

export interface AdminLoginPayload {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface AdminLoginProfile {
  id: string;
  fullName: string;
  email: string;
  role: string;
  lastLoginAt: string;
}

export interface AdminLoginData {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  admin: AdminLoginProfile;
}

export interface AdminRefreshPayload {
  refreshToken: string;
}

export type AdminRefreshData = AdminLoginData;

export interface AdminLogoutPayload {
  refreshToken: string;
}

export interface AdminLogoutData {
  loggedOut: boolean;
}

export interface AdminDashboardSummary {
  totalEmployers: number;
  totalJobSeekers: number;
  pendingJobs: number;
  totalRevenue: number;
  growth: {
    employersPct: number;
    jobSeekersPct: number;
    revenuePct: number;
  };
}

export interface AdminRevenueTrend {
  range: string;
  labels: string[];
  current: number[];
  previous: number[];
}

export interface AdminJobDistribution {
  total: number;
  activePct: number;
  pendingPct: number;
  expiredPct: number;
}

export interface AdminPendingJobItem {
  id: string;
  title: string;
  subtitle: string;
  company: string;
  postDate: string;
  status: string;
}

export interface AdminPendingJobsQuery {
  page: number;
  pageSize: number;
  search?: string;
  status?: string;
}

export interface AdminEmployersMetrics {
  totalEmployers: number;
  totalEmployersGrowthPct: number;
  kycVerified: number;
  kycVerifiedPct: number;
  pendingKyc: number;
  suspended: number;
}

export interface AdminEmployerListQuery {
  page: number;
  pageSize: number;
  search?: string;
  kycStatus?: string;
  status?: string;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

export interface AdminEmployerItem {
  id: string;
  name: string;
  industry: string;
  registrationDate: string;
  activeJobs: number;
  kycStatus: string;
  accountStatus: string;
  avatarInitials: string;
}

export interface AdminEmployerDetail extends AdminEmployerItem {
  contactEmail: string;
  contactPhone: string;
}

export type AdminEmployerStatusAction = 'suspend' | 'restore' | 'activate';

export interface AdminUpdateEmployerStatusPayload {
  action: AdminEmployerStatusAction;
  reason?: string;
}

export interface AdminUpdateEmployerStatusData {
  id: string | number;
  updated: boolean;
}

export interface AdminEmployersExportQuery {
  format?: 'csv';
  search?: string;
  kycStatus?: string;
  status?: string;
}

export interface AdminEmployersExportData {
  downloadUrl: string;
  expiresAt?: string;
}

export interface AdminJobSeekersMetrics {
  totalSeekers: number;
  totalSeekersGrowthPct: number;
  activeLast7Days: number;
  placedCandidates: number;
  retentionPct: number;
}

export interface AdminJobSeekerListQuery {
  page: number;
  pageSize: number;
  search?: string;
  resumeStatus?: string;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

export interface AdminJobSeekerItem {
  id: string;
  fullName: string;
  email: string;
  profession: string;
  resumeStatus: string;
  lastActiveAt: string;
  avatarInitials: string;
}

export interface AdminCreateJobSeekerPayload {
  fullName: string;
  email: string;
  profession: string;
  resumeUrl: string;
}

export interface AdminCreateJobSeekerData {
  id: string;
  created: boolean;
}

export interface AdminRegionDistribution {
  regions: Array<{
    code: string;
    count: number;
  }>;
}

export interface AdminJobsMetrics {
  livePostings: number;
  livePostingsGrowthPct: number;
  pendingReview: number;
  totalApplicants: number;
  avgTimeToHireDays: number;
}

export interface AdminJobsListQuery {
  page: number;
  pageSize: number;
  search?: string;
  category?: string;
  status?: string;
  sortBy?: string;
  sortDir?: AdminSortDirection;
}

export interface AdminJobItem {
  id: string;
  title: string;
  company: string;
  location: string;
  category: string;
  applications: number;
  newApplicationsToday: number;
  status: string;
  expiryDate: string;
}

export interface AdminCreateJobPayload {
  title: string;
  companyId: string;
  category: string;
  description: string;
  location: string;
  expiryDate: string;
}

export interface AdminCreateJobData {
  id: string;
  created: boolean;
  status?: string;
}

export interface AdminUpdateJobStatusPayload {
  status: string;
}

export interface AdminUpdateJobStatusData {
  id: string;
  status: string;
  updatedAt: string;
}

export interface AdminBulkJobActionPayload {
  jobIds: string[];
  action: string;
}

export interface AdminBulkJobActionData {
  processed: number;
  failed: number;
}

export interface AdminBillingTier {
  id: string;
  name: string;
  badge: string;
  priceMonthly: number;
  currency: string;
  isPopular: boolean;
  usagePct: number;
  features: string[];
}

export interface AdminBillingTiersData {
  tiers: AdminBillingTier[];
}

export interface AdminUpdateBillingTierPayload {
  priceMonthly: number;
  features: string[];
}

export interface AdminUpdateBillingTierData {
  id: string;
  updated: boolean;
  updatedAt?: string;
}

export interface AdminBillingTransactionsQuery {
  page: number;
  pageSize: number;
  status?: string;
  from?: string;
  to?: string;
}

export interface AdminBillingTransactionItem {
  id: string;
  employerId: string;
  employerName: string;
  packageName: string;
  amount: number;
  currency: string;
  date: string;
  status: string;
}

export interface AdminBillingSummary {
  monthlyRecurringRevenue: number;
  mrrGrowthPct: number;
  activeSubscriptions: number;
}
