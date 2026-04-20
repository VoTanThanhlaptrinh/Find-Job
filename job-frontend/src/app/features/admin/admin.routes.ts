import { Routes } from '@angular/router';

import { AdminLoginComponent } from './pages/admin-login/admin-login.component';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { BillingComponent } from './pages/billing/billing.component';
import { EmployersComponent } from './pages/employers/employers.component';
import { JobSeekersComponent } from './pages/job-seekers/job-seekers.component';
import { JobsManagementComponent } from './pages/jobs-management/jobs-management.component';
import { OverviewComponent } from './pages/overview/overview.component';

export const adminRoutes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'admin-login', component: AdminLoginComponent },
  { 
    path: '', 
    component: DashboardComponent,
    children: [
      { path: 'dashboard', component: OverviewComponent },
      { path: 'employers', component: EmployersComponent },
      { path: 'job-seekers', component: JobSeekersComponent },
      { path: 'jobs', component: JobsManagementComponent },
      { path: 'billing', component: BillingComponent },
    ]
  },
  { path: '**', redirectTo: 'login' }
];
