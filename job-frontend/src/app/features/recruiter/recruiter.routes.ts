import { Routes } from '@angular/router';

import { hirerChildGuard } from '../../core/guards/hirer-guard.guard';
import { CandidateListComponent } from './pages/candidate-list/candidate-list.component';
import { CompanyAddressComponent } from './pages/company-address/company-address.component';
import { RecruiterDashboardComponent } from './pages/recruiter-dashboard/recruiter-dashboard.component';
import { RecruiterJobDetailComponent } from './pages/recruiter-job-detail/recruiter-job-detail.component';
import { RecruiterJobListComponent } from './pages/recruiter-job-list/recruiter-job-list.component';
import { RecruiterLoginComponent } from './pages/recruiter-login/recruiter-login.component';
import { PostJobComponent } from './pages/recruiter-post-job/recruiter-post-job.component';
import { RecruiterRegisterComponent } from './pages/recruiter-register/recruiter-register.component';

export const recruiterRoutes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: RecruiterLoginComponent },
  { path: 'register', component: RecruiterRegisterComponent },

  { path: 'dashboard/overview', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard/jobs', redirectTo: 'jobs', pathMatch: 'full' },
  { path: 'dashboard/jobs/post-job', redirectTo: 'jobs/post-job', pathMatch: 'full' },
  { path: 'dashboard/jobs/detail/:id', redirectTo: 'jobs/detail/:id', pathMatch: 'full' },
  { path: 'dashboard/candidates', redirectTo: 'candidates', pathMatch: 'full' },
  { path: 'dashboard/company-address', redirectTo: 'company-address', pathMatch: 'full' },

  {
    path: '',
    canActivateChild: [hirerChildGuard],
    children: [
      { path: 'dashboard', component: RecruiterDashboardComponent },
      { path: 'jobs', component: RecruiterJobListComponent },
      { path: 'jobs/detail/:id', component: RecruiterJobDetailComponent },
      { path: 'jobs/post-job', component: PostJobComponent },
      { path: 'candidates', component: CandidateListComponent },
      { path: 'company-address', component: CompanyAddressComponent },
    ],
  },

  { path: '**', redirectTo: 'login' },
];
