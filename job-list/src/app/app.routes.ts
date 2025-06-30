import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AboutUsComponent } from './about-us/about-us.component';
import { BlogHomeComponent } from './blog-home/blog-home.component';
import { BlogSingleComponent } from './blog-single/blog-single.component';
import { ContactComponent } from './contact/contact.component';
import { CategoryComponent } from './category/category.component';
import { InforComponent } from './infor/infor.component';
import { ChangePassComponent } from './change-pass/change-pass.component';
import { CvUiComponent } from './cv-ui/cv-ui.component';
import { HistoryApplyComponent } from './apply-history/history-apply.component';
import { ProfileComponent } from './profile/profile.component';
import { VerifyComponent } from './verify/verify.component';
import { ActivateComponent } from './activate/activate.component';
import { JobSingleComponent } from './job-single/job-single.component';
import { ApplyCvComponent } from './apply-cv/apply-cv.component';
import {LoginCallbackComponent} from './login-callback/login-callback.component';
import {PostJobComponent} from './post-job/post-job.component';
import {ForgotPassComponent} from './forgot-pass/forgot-pass.component';
import {ResetPassComponent} from './reset-pass/reset-pass.component';
export const routes: Routes = [
    {path:'', component:HomeComponent},
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
          { path: '', component: ProfileComponent },
          { path: 'change-password', component: ChangePassComponent },
          { path: 'cv', component: CvUiComponent },
          { path: 'history-apply', component: HistoryApplyComponent }
        ]
      },
      {path:'single/:id', component:JobSingleComponent},
      {path:'apply-cv/:id', component:ApplyCvComponent},
];
