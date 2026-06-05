import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home/home.component';
import { userLoginGuard } from './core/guards/user-login.guard';
import { hirerGuard } from './core/guards/hirer-guard.guard';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    {
      path: 'auth',
      children: [
        {
          path: 'login',
          loadComponent: () => import('./features/auth/pages/login/login.component').then(m => m.LoginComponent)
        },
        {
          path: 'register',
          loadComponent: () => import('./features/auth/pages/register/register.component').then(m => m.RegisterComponent)
        },
        {
          path: 'verify',
          loadComponent: () => import('./features/auth/pages/verify/verify.component').then(m => m.VerifyComponent)
        },
        {
          path: 'activate',
          loadComponent: () => import('./features/auth/pages/activate/activate.component').then(m => m.ActivateComponent)
        },
        {
          path: 'forgot-pass',
          loadComponent: () => import('./features/auth/pages/forgot-pass/forgot-pass.component').then(m => m.ForgotPassComponent)
        },
        {
          path: 'reset-pass/:random',
          loadComponent: () => import('./features/auth/pages/reset-pass/reset-pass.component').then(m => m.ResetPassComponent)
        },
        {
          path: 'login-callback',
          loadComponent: () => import('./features/auth/pages/login-callback/login-callback.component').then(m => m.LoginCallbackComponent)
        }
      ]
    },
    {
      path: 'infor',
      loadComponent: () => import('./features/auth/candidate/infor/infor.component').then(m => m.InforComponent),
      children: [
        {
          path: '',
          loadComponent: () => import('./features/auth/candidate/profile/profile.component').then(m => m.ProfileComponent),
          canActivate: [userLoginGuard]
        },
        {
          path: 'change-password',
          loadComponent: () => import('./features/auth/candidate/change-pass/change-pass.component').then(m => m.ChangePassComponent)
        },
        {
          path: 'cv',
          loadComponent: () => import('./features/auth/candidate/cv-ui/cv-ui.component').then(m => m.CvUiComponent)
        },
        {
          path: 'history-apply',
          loadComponent: () => import('./features/jobs/pages/apply-history/history-apply.component').then(m => m.HistoryApplyComponent)
        }
      ]
    },
    {
      path: 'site',
      children: [
        {
          path: 'about',
          loadComponent: () => import('./features/site/pages/about-us/about-us.component').then(m => m.AboutUsComponent)
        },
        {
          path: 'contact',
          loadComponent: () => import('./features/site/pages/contact/contact.component').then(m => m.ContactComponent)
        }
      ]
    },
    {
      path: 'blog',
      children: [
        {
          path: '',
          loadComponent: () => import('./features/blog/pages/blog-home/blog-home.component').then(m => m.BlogHomeComponent)
        },
        {
          path: 'single/:id',
          loadComponent: () => import('./features/blog/pages/blog-single/blog-single.component').then(m => m.BlogSingleComponent)
        }
      ]
    },
    {
      path: 'jobs',
      children: [
        {
          path: 'category',
          loadComponent: () => import('./features/jobs/pages/category/category.component').then(m => m.CategoryComponent)
        },
        {
          path: 'single/:id',
          loadComponent: () => import('./features/jobs/pages/job-single/job-single.component').then(m => m.JobSingleComponent)
        },
        {
          path: 'apply-cv/:id',
          loadComponent: () => import('./features/jobs/pages/apply-cv/apply-cv.component').then(m => m.ApplyCvComponent)
        }
      ]
    },
    {
      path: 'recruiter',
      children: [
        {
          path: 'login',
          loadComponent: () => import('./features/employer/pages/recruiter-login/recruiter-login.component').then(m => m.RecruiterLoginComponent)
        },
        {
          path: 'register',
          loadComponent: () => import('./features/employer/pages/recruiter-register/recruiter-register.component').then(m => m.RecruiterRegisterComponent)
        },
        {
          path: 'dashboard',
          loadComponent: () => import('./features/employer/pages/recruiter-dashboard/recruiter-dashboard.component').then(m => m.RecruiterDashboardComponent),
          canActivate: [hirerGuard]
        }
      ]
    },
    {
      path: 'employer',
      canActivate: [hirerGuard],
      children: [
        {
          path: 'post-job',
          loadComponent: () => import('./features/employer/pages/post-job/post-job.component').then(m => m.PostJobComponent)
        },
        {
          path: 'hirer',
          loadComponent: () => import('./features/employer/pages/hirer-home/hirer-home.component').then(m => m.HirerHomeComponent)
        },
        {
          path: 'update-job',
          loadComponent: () => import('./features/employer/pages/update-job/update-job.component').then(m => m.UpdateJobComponent)
        },
        {
          path: 'candidate-list',
          loadComponent: () => import('./features/employer/pages/candidate-list/candidate-list.component').then(m => m.CandidateListComponent)
        }
      ]
    },
    // Legacy routes - redirect
    { path: 'login', redirectTo: '/auth/login', pathMatch: 'full' },
    { path: 'register', redirectTo: '/auth/register', pathMatch: 'full' },
    { path: 'about', redirectTo: '/site/about', pathMatch: 'full' },
    { path: 'contact', redirectTo: '/site/contact', pathMatch: 'full' },
    { path: 'blogHome', redirectTo: '/blog', pathMatch: 'full' },
    { path: 'blogSingle/:id', redirectTo: '/blog/single/:id', pathMatch: 'full' },
    { path: 'category', redirectTo: '/jobs/category', pathMatch: 'full' },
    { path: 'single/:id', redirectTo: '/jobs/single/:id', pathMatch: 'full' },
    { path: 'apply-cv/:id', redirectTo: '/jobs/apply-cv/:id', pathMatch: 'full' },
    { path: 'hirer', redirectTo: '/employer/hirer', pathMatch: 'full' },
    { path: 'update-job', redirectTo: '/employer/update-job', pathMatch: 'full' },
    { path: 'candidate-list', redirectTo: '/employer/candidate-list', pathMatch: 'full' },
    { path: 'post-job', redirectTo: '/employer/post-job', pathMatch: 'full' },
    { path: 'forgot-pass', redirectTo: '/auth/forgot-pass', pathMatch: 'full' },
    { path: 'reset-pass/:random', redirectTo: '/auth/reset-pass/:random', pathMatch: 'full' },
    { path: 'verify', redirectTo: '/auth/verify', pathMatch: 'full' },
    { path: 'activate', redirectTo: '/auth/activate', pathMatch: 'full' },
];
