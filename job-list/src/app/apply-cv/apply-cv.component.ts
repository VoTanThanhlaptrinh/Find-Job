import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobServiceService } from '../services/job-service.service';
import { AuthService } from '../services/auth.service';
import { NgIf } from '@angular/common';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import { take } from 'rxjs';
import {NotifyMessageService} from '../services/notify-message.service';
import {response} from 'express';


@Component({
  selector: 'app-apply-cv',
  imports: [RouterModule, FormsModule, NgIf,ReactiveFormsModule],
  templateUrl: './apply-cv.component.html',
  styleUrl: './apply-cv.component.css'
})
export class ApplyCvComponent implements OnInit {
  job:any
  jobId!: string;
  selectedFile: File | null = null;
  fileName!: string;
  formGroup = new FormGroup({
    file: new FormControl<File | null>(null),
  })
  constructor(private router:Router,
              private route:ActivatedRoute,
              private jobService: JobServiceService,
              private authService: AuthService,
              private notify: NotifyMessageService
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
      this.getDetailJob(this.jobId);
    });
  }

  getDetailJob(id: string) {
    this.jobService.getDetailJob(id).subscribe({
      next: (response: any) => {
        this.job= response.data;
        console.log(this.job)
      },
      error: (error) => {
        console.error('Error fetching job details:', error);
        this.router.navigate(['/']); // Redirect to home if error occurs
      }
    });
  }
  onSelectedFile(event: Event){
    const inputFile = event.target as HTMLInputElement;
    if(inputFile.files?.length){
      this.selectedFile = inputFile.files[0];
      this.formGroup.patchValue({ file: this.selectedFile});

    }
  }
  checkFileExtension(file:File):boolean{
    const fileName = file.name;
    const extension = fileName.substring(fileName.lastIndexOf(".")+1);
    return extension === 'doc' || extension === 'docx' || extension === 'pdf'
  }
  submitCV(): void{
    if(!this.selectedFile){
      this.notify.showMessage("Thiếu file cv!",'','warning');
      return;
    }
    if(!this.checkFileExtension(this.selectedFile)){
      this.notify.showMessage("Sai extension !",'','warning');
      return;
    }
    if(this.selectedFile.size > 5 * 1024 * 1024)
    if(!this.jobId){
      this.notify.showMessage("file chỉ tối đa 5mb!",'','warning');
      return;
    }
    const formData = new FormData();
    const file = this.selectedFile;
    if(file){
      formData.append('file', file);
    }
    formData.append('jobId', this.jobId);
    this.jobService.applyCV(formData).pipe(take(1)).subscribe({
      next: (res) => {
        this.notify.showMessage(res.message,'','success');
        this.formGroup.reset();
      },
      error: (err) => {
        console.error(err);
        const msg = err?.error?.message || err?.message || 'Có lỗi xảy ra, vui lòng thử lại!';
        this.notify.showMessage(msg, '', 'error');
      }
    })
  }

}
