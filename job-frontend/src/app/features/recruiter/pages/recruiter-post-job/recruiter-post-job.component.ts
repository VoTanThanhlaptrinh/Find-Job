import { Component, effect } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { QuillModule } from 'ngx-quill';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
@Component({
  selector: 'app-post-job',
  imports: [FormsModule
    , QuillModule
    , ReactiveFormsModule
    , CommonModule],
  templateUrl: './recruiter-post-job.component.html',
  styleUrl: './recruiter-post-job.component.css'
})

export class PostJobComponent {
  messageType: boolean | undefined = undefined;
  message = '';
  postJobFG = new FormGroup({
    jobName: new FormControl('', [Validators.required]),
    location: new FormControl('', [Validators.required]),
    jobType: new FormControl('Full Time', [Validators.required]),
    salary: new FormControl('', [Validators.required, Validators.min(2000000)]),
    headCount: new FormControl('', [Validators.required, Validators.min(1)]),
    jobDescription: new FormControl('', [Validators.required]),
    jobRequirement: new FormControl('', [Validators.required]),
    jobSkill: new FormControl('', [Validators.required]),
    deadlineCV: new FormControl<Date|null>(null, [Validators.required, minDatePlusOne]),
    companyName: new FormControl('', [Validators.required]),
    companyDescription: new FormControl(''),
    compayWebsite: new FormControl(''),
    image: new FormControl<File | null>(null, Validators.required)
  });

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService
  ) {
    effect(() => {
      const actionTick = this.recruiterJobsService.actionTick$();
      const actionType = this.recruiterJobsService.lastActionType$();
      if (actionTick === 0 || actionType !== 'create') {
        return;
      }

      const isSuccess = this.recruiterJobsService.lastActionSuccess$();
      const message = this.recruiterJobsService.lastActionMessage$();
      this.messageType = isSuccess;
      this.message = message;

      if (isSuccess) {
        this.notify.showMessage(message, '', 'success');
        this.postJobFG.reset({
          jobType: 'Full Time',
          deadlineCV: null,
          image: null
        });
        return;
      }

      this.notify.showMessage(message, '', 'error');
    });
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
          } else if (val instanceof Date) {
            formData.append(key, val.toISOString().split('T')[0]);
          } else {
            formData.append(key, val);
          }
      }
    });
    this.recruiterJobsService.createJob(formData);
  }
  onFilePicked(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.postJobFG.get('image')?.setValue(file, { emitModelToViewChange: false });
    }
  }
  get f() {
    return this.postJobFG.controls;
  }

  get isSubmitting(): boolean {
    return this.recruiterJobsService.isSubmittingJob$();
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


