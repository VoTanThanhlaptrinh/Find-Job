import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobServiceService } from '../services/job-service.service';
import { AuthService } from '../services/auth.service';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { take } from 'rxjs';


@Component({
  selector: 'app-apply-cv',
  imports: [RouterModule, FormsModule, NgIf],
  templateUrl: './apply-cv.component.html',
  styleUrl: './apply-cv.component.css'
})
export class ApplyCvComponent implements OnInit {
  jobDetail:any
  jobId!: string;
  selectedFile: File | null = null;
  constructor(private router:Router,
              private route:ActivatedRoute,
              private jobService: JobServiceService,
              private authService: AuthService,
              private toastr: ToastrService
              ) {

  }
  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe(params => {
      if (!this.authService.isLogin()) {
        this.router.navigate(['/login'],
          {
          queryParams: {
            message: 'Bạn cần đăng nhập để tiếp tục',
            returnUrl: this.router.url  ,
            status: 'error'
          }
        });
        return;
      }
      this.jobId = params['id'];
      this.checkApplyJob();
      this.getDetailJob(this.jobId);
    });
  }
  checkApplyJob() {
    this.jobService.checkApplyJob(this.jobId).pipe(take(1)).subscribe({
      next: (response: any) => {
        if (!response.data) {
           this.router.navigate(['/']);
        }
      },
      error: (error) => {
        window.location.href = '/';
      }
    });
  }
  getDetailJob(id: string) {
    this.jobService.getDetailJob(id).subscribe({
      next: (response: any) => {
        this.jobDetail = response.data;
      },
      error: (error) => {
        console.error('Error fetching job details:', error);
        window.location.href = '/'; // Redirect to home if error occurs
      }
    });
  }
  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
  }
}
