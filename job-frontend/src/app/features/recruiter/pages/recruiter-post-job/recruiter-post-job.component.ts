import { Component, effect, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from '@angular/forms';
import { QuillModule } from 'ngx-quill';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterAddressService } from '../../services/recruiter-address.service';
import { CompanyAddress } from '../company-address/company-address.component';

@Component({
  selector: 'app-post-job',
  imports: [
    FormsModule,
    QuillModule,
    ReactiveFormsModule,
    CommonModule,
    TranslatePipe
  ],
  templateUrl: './recruiter-post-job.component.html',
  styleUrl: './recruiter-post-job.component.css'
})

export class PostJobComponent implements OnInit {
  messageType: boolean | undefined = undefined;
  message = '';
  postJobFG = new FormGroup({
    jobName: new FormControl('', [Validators.required]),
    location: new FormControl('', [Validators.required]),
    jobType: new FormControl('FULL_TIME', [Validators.required]),
    salary: new FormControl('', [Validators.required, Validators.min(2000000)]),
    headCount: new FormControl('', [Validators.required, Validators.min(1)]),
    jobDescription: new FormControl('', [Validators.required]),
    jobRequirement: new FormControl('', [Validators.required]),
    jobSkill: new FormControl('', [Validators.required]),
    moreDetail: new FormControl(''),
    deadlineCV: new FormControl<Date|null>(null, [Validators.required, minDatePlusOne]),
    enableAiAnalysis: new FormControl(false)
  });

  companyAddresses: CompanyAddress[] = [];

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService,
    private readonly addressService: RecruiterAddressService
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
          jobType: 'FULL_TIME',
          deadlineCV: null,
          enableAiAnalysis: false
        });
        return;
      }

      this.notify.showMessage(message, '', 'error');
    });
  }

  ngOnInit(): void {
    this.addressService.getAddresses().subscribe({
      next: (response) => {
        if (response.status === 200) {
          this.companyAddresses = response.data;
          
          // Set default address if available
          const defaultAddress = this.companyAddresses.find(a => a.isDefault);
          if (defaultAddress) {
            this.postJobFG.get('location')?.setValue(defaultAddress.id.toString());
          }
        }
      },
      error: (err) => {
        console.error('Failed to load company addresses', err);
      }
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
          let formKey = key;
          if (key === 'location') formKey = 'addressId';
          if (key === 'headCount') formKey = 'headcount';
          if (val instanceof File) {
            formData.append(formKey, val, val.name);
          } else if (val instanceof Date) {
            formData.append(formKey, val.toISOString().split('T')[0]);
          } else if (typeof val === 'boolean') {
            formData.append(formKey, String(val));
          } else {
            formData.append(formKey, String(val));
          }
      }
    });
    this.recruiterJobsService.createJob(formData);
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


