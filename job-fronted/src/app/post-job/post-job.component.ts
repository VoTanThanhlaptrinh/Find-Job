import {Component, inject} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from '@angular/forms';
import {JobServiceService} from '../services/job-service.service';
import {RouterLink} from '@angular/router';
import {QuillModule} from 'ngx-quill';
import {CommonModule} from '@angular/common';
import {NotifyMessageService} from '../services/notify-message.service';
@Component({
  selector: 'app-post-job',
  imports: [FormsModule
    , RouterLink
    , QuillModule
    , ReactiveFormsModule
    , CommonModule],
  templateUrl: './post-job.component.html',
  styleUrl: './post-job.component.css'
})

export class PostJobComponent {
  messageType: boolean | undefined = undefined;
  message: string = '';
  postJobFG = new FormGroup({
    jobName: new FormControl('', [Validators.required]),
    location: new FormControl('', [Validators.required]),
    jobType: new FormControl('', [Validators.required]),
    salary: new FormControl('', [Validators.required, Validators.min(2000000)]),
    jobDescription: new FormControl('', [Validators.required]),
    jobRequirement: new FormControl('', [Validators.required]),
    jobSkill: new FormControl('', [Validators.required]),
    deadlineCV: new FormControl<Date|null>(null, [Validators.required, minDatePlusOne]),
    companyName: new FormControl('', [Validators.required]),
    companyDescription: new FormControl(''),
    compayWebsite: new FormControl(''),
    image: new FormControl<File | null>(null, Validators.required)
  });

  constructor(private jobService: JobServiceService
              ,private notify: NotifyMessageService) {
  }
  onSubmit() {
    if(this.postJobFG.invalid){
      this.postJobFG.markAllAsTouched();
      return;
    }
    const formValue = this.postJobFG.value;
    const formData = new FormData();
    Object.entries(formValue).forEach(([key, val]) => {
      if (val !== null && val !== undefined) {
          if (val instanceof File) {
            formData.append(key, val, val.name);
          }
          if (val instanceof Date) {
            formData.append(key,val.toISOString().split('T')[0]);
          }else{
            formData.append(key, val);
          }
      }
    });
    this.jobService.doPostJob(formData).subscribe({
      next: res =>{
        this.messageType = (res.status === 200);
        this.notify.showMessage(res.message,'','success')
      }, error: err => {
        this.notify.showMessage(err?.error?.message,'','error')
      }
    })
  }
  onFilePicked(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      // Khi patch file cần thêm opt emitModelToViewChange: false để tránh lỗi DOMException
      this.postJobFG.get('image')?.setValue(file, { emitModelToViewChange: false });
    }
  }
  get f() {
    return this.postJobFG.controls;
  }
}
export function minDatePlusOne(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;

  const selected = new Date(value);
  const today = new Date();
  const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);

  if (isNaN(selected.getTime()) || selected <= tomorrow) {
    return { minDatePlusOne: { requiredDate: tomorrow.toISOString().split('T')[0] } };
  }
  return null;
}


