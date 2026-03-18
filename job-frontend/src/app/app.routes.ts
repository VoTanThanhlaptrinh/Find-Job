import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home/home.component';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { AboutUsComponent } from './features/site/pages/about-us/about-us.component';
import { BlogHomeComponent } from './features/blog/pages/blog-home/blog-home.component';
import { BlogSingleComponent } from './features/blog/pages/blog-single/blog-single.component';
import { ContactComponent } from './features/site/pages/contact/contact.component';
import { CategoryComponent } from './features/jobs/pages/category/category.component';
import { InforComponent } from './features/auth/candidate/infor/infor.component';
import { ChangePassComponent } from './features/auth/candidate/change-pass/change-pass.component';
import { CvUiComponent } from './features/auth/candidate/cv-ui/cv-ui.component';
import { HistoryApplyComponent } from './features/jobs/pages/apply-history/history-apply.component';
import { ProfileComponent } from './features/auth/candidate/profile/profile.component';
import { VerifyComponent } from './features/auth/pages/verify/verify.component';
import { ActivateComponent } from './features/auth/pages/activate/activate.component';
import { JobSingleComponent } from './features/jobs/pages/job-single/job-single.component';
import { ApplyCvComponent } from './features/jobs/pages/apply-cv/apply-cv.component';
import {LoginCallbackComponent} from './features/auth/pages/login-callback/login-callback.component';
import {PostJobComponent} from './features/employer/pages/post-job/post-job.component';
import {ForgotPassComponent} from './features/auth/pages/forgot-pass/forgot-pass.component';
import {ResetPassComponent} from './features/auth/pages/reset-pass/reset-pass.component';
import {HirerHomeComponent} from './features/employer/pages/hirer-home/hirer-home.component';
import {UpdateJobComponent} from './features/employer/pages/update-job/update-job.component';
import {CandidateListComponent} from './features/employer/pages/candidate-list/candidate-list.component';
import { userLoginGuard } from './core/guards/user-login.guard';
import { hirerGuard } from './core/guards/hirer-guard.guard';
import { RecruiterLoginComponent } from './features/employer/pages/recruiter-login/recruiter-login.component';
import { RecruiterRegisterComponent } from './features/employer/pages/recruiter-register/recruiter-register.component';
import { RecruiterDashboardComponent } from './features/employer/pages/recruiter-dashboard/recruiter-dashboard.component';
export const routes: Routes = [
    {path:'', component:HomeComponent},
  {path:'recruiter/login', component:RecruiterLoginComponent},
  {path:'recruiter/register', component:RecruiterRegisterComponent},
  {path:'recruiter/dashboard', component:RecruiterDashboardComponent},
    {path:'login', component:LoginComponent},
    {path:'register', component:RegisterComponent},
    {path:'about', component:AboutUsComponent},
    {path:'blogHome', component:BlogHomeComponent},
    {path:'blogSingle/:id', component:BlogSingleComponent},
    {path:'contact', component:ContactComponent},
    {path:'category', component:CategoryComponent},
    {path:'verify', component:VerifyComponent},
    {path:'activate', component:ActivateComponent},
    {path:'post-job', component:PostJobComponent},
    {path:'forgot-pass', component:ForgotPassComponent},
    {path:'reset-pass/:random', component:ResetPassComponent},
    {path:'login-callback', component:LoginCallbackComponent},
    {
        path: 'infor',
        component: InforComponent,
        
        children: [
          { path: '', component: ProfileComponent,canActivate:[userLoginGuard], },
          { path: 'change-password', component: ChangePassComponent },
          { path: 'cv', component: CvUiComponent },
          { path: 'history-apply', component: HistoryApplyComponent }
        ]
      },
      {
        path: 'hirer',
        component: HirerHomeComponent,
        canActivate: [hirerGuard],
      },
      {
        path: 'update-job',
        component : UpdateJobComponent,
        canActivate: [hirerGuard],
      },
      {
        path: 'candidate-list',
        component: CandidateListComponent,
        canActivate: [hirerGuard],
      }
      ,
      {path:'single/:id', component:JobSingleComponent},
      {path:'apply-cv/:id', component:ApplyCvComponent},
];
