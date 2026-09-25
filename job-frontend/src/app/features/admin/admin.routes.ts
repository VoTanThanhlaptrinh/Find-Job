import { Routes } from '@angular/router';
import { adminChildGuard, adminGuard } from '../../core/guards/admin-guard.guard';

import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BillingComponent } from './pages/billing/billing.component';
import { EmployersComponent } from './pages/employers/employers.component';
import { JobSeekersComponent } from './pages/job-seekers/job-seekers.component';
import { JobsManagementComponent } from './pages/jobs-management/jobs-management.component';
import { OverviewComponent } from './pages/overview/overview.component';
import { AdminRouteData } from './services/admin-api.models';

export const adminRoutes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { 
    path: '', 
    component: DashboardComponent,
    canActivate: [adminGuard],
    canActivateChild: [adminChildGuard],
    children: [
      {
        path: 'dashboard',
        component: OverviewComponent,
        data: {
          title: 'Tổng quan hệ thống',
          breadcrumb: 'Tổng quan',
          navigationGroup: 'overview',
          navigationIcon: 'dashboard',
        } as AdminRouteData,
      },
      {
        path: 'employers',
        component: EmployersComponent,
        data: {
          title: 'Quản lý nhà tuyển dụng',
          breadcrumb: 'Nhà tuyển dụng',
          navigationGroup: 'user-operations',
          navigationIcon: 'employers',
        } as AdminRouteData,
      },
      {
        path: 'job-seekers',
        component: JobSeekersComponent,
        data: {
          title: 'Quản lý người tìm việc',
          breadcrumb: 'Người tìm việc',
          navigationGroup: 'user-operations',
          navigationIcon: 'job-seekers',
        } as AdminRouteData,
      },
      {
        path: 'jobs',
        component: JobsManagementComponent,
        data: {
          title: 'Quản lý tin tuyển dụng',
          breadcrumb: 'Tin tuyển dụng',
          navigationGroup: 'recruitment',
          navigationIcon: 'jobs',
        } as AdminRouteData,
      },
      {
        path: 'billing',
        component: BillingComponent,
        data: {
          title: 'Thanh toán & Gói dịch vụ',
          breadcrumb: 'Gói dịch vụ',
          requiredPermission: 'billing.read',
          navigationGroup: 'revenue',
          navigationIcon: 'billing',
        } as AdminRouteData,
      },
    ]
  },
  { path: '**', redirectTo: 'login' }
];
