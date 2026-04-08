import { Routes } from '@angular/router';
import { PostJobComponent } from '../recruiter-post-job/recruiter-post-job.component';
import { RecruiterDashboardComponent } from './recruiter-dashboard.component';
import { RecruiterDashboardOverviewComponent } from '../recruiter-dashboard-overview/recruiter-dashboard-overview.component';
import { RecruiterJobListComponent } from '../recruiter-job-list/recruiter-job-list.component';
import { CompanyAddressComponent } from '../company-address/company-address.component';
import { CandidateListComponent } from '../candidate-list/candidate-list.component';
import { RecruiterJobDetailComponent } from '../recruiter-job-detail/recruiter-job-detail.component';
import { hirerChildGuard, hirerGuard } from '../../../../core/guards/hirer-guard.guard';

export const recruiterDashboardRoutes: Routes = [
  {
    path: 'recruiter/dashboard',
    component: RecruiterDashboardComponent,
    canActivate: [hirerGuard],
    canActivateChild: [hirerChildGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'overview' },
      { path: 'overview', component: RecruiterDashboardOverviewComponent },
      { path: 'jobs', component: RecruiterJobListComponent },
      { path: 'jobs/detail/:id', component: RecruiterJobDetailComponent },
      { path: 'jobs/post-job', component: PostJobComponent },
      { path: 'candidates', component: CandidateListComponent },
      { path: 'company-address', component: CompanyAddressComponent },
    ],
  },
];
