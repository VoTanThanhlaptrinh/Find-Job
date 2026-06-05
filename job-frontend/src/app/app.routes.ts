import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/pages/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },

  // Lazy load Children - Các nhóm module lớn đã được chia file routes riêng
  {
    path: 'recruiter',
    loadChildren: () => import('./features/recruiter/recruiter.routes').then((m) => m.recruiterRoutes)
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.adminRoutes)
  },

  { path: 'post-job', redirectTo: 'recruiter/jobs/post-job', pathMatch: 'full' },
  { path: 'candidate-list', redirectTo: 'recruiter/candidates', pathMatch: 'full' },

  { path: 'login', loadComponent: () => import('./features/auth/pages/login/login.component').then(c => c.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/pages/register/register.component').then(c => c.RegisterComponent) },
  { path: 'verify', loadComponent: () => import('./features/auth/pages/verify/verify.component').then(c => c.VerifyComponent) },
  { path: 'activate', loadComponent: () => import('./features/auth/pages/activate/activate.component').then(c => c.ActivateComponent) },
  { path: 'forgot-pass', loadComponent: () => import('./features/auth/pages/forgot-pass/forgot-pass.component').then(c => c.ForgotPassComponent) },
  { path: 'reset-pass/:random', loadComponent: () => import('./features/auth/pages/reset-pass/reset-pass.component').then(c => c.ResetPassComponent) },
  { path: 'login-callback', loadComponent: () => import('./features/auth/pages/login-callback/login-callback.component').then(c => c.LoginCallbackComponent) },

  // Lazy load Components - Tính năng Site / Blog
  { path: 'about', loadComponent: () => import('./features/site/pages/about-us/about-us.component').then(c => c.AboutUsComponent) },
  { path: 'contact', loadComponent: () => import('./features/site/pages/contact/contact.component').then(c => c.ContactComponent) },
  { path: 'blogHome', loadComponent: () => import('./features/blog/pages/blog-home/blog-home.component').then(c => c.BlogHomeComponent) },
  { path: 'blogSingle/:id', loadComponent: () => import('./features/blog/pages/blog-single/blog-single.component').then(c => c.BlogSingleComponent) },

  // Lazy load Components - Tính năng Việc làm
  { path: 'category', loadComponent: () => import('./features/jobs/pages/category/category.component').then(c => c.CategoryComponent) },
  { path: 'single/:id', loadComponent: () => import('./features/jobs/pages/job-single/job-single.component').then(c => c.JobSingleComponent) },
  { path: 'apply-cv/:id', loadComponent: () => import('./features/jobs/pages/apply-cv/apply-cv.component').then(c => c.ApplyCvComponent) },
  { path: 'apply-success', loadComponent: () => import('./shared/components/apply-success/apply-success.component').then(c => c.ApplySuccessComponent) },

  // Lazy load - Dashboard của Candidate & Các trang con
  {
    path: 'infor',
    loadComponent: () => import('./features/candidate/infor/infor.component').then(c => c.InforComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      { path: 'profile', loadComponent: () => import('./features/candidate/profile/profile.component').then(c => c.ProfileComponent) },
      { path: 'change-password', loadComponent: () => import('./features/candidate/change-pass/change-pass.component').then(c => c.ChangePassComponent) },
      { path: 'cv', loadComponent: () => import('./features/candidate/cv-ui/cv-ui.component').then(c => c.CvUiComponent) },
      { path: 'recommended-jobs', loadComponent: () => import('./features/candidate/recommended-jobs/recommended-jobs.component').then(c => c.RecommendedJobsComponent) },
      { path: 'history-apply', loadComponent: () => import('./features/candidate/history-apply/history-apply.component').then(c => c.HistoryApplyComponent) }
    ]
  }
];