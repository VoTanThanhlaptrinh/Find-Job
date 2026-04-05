import { Component, effect, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobService } from '../../services/job.service';
import { ResumeService } from '../../../../core/services/resume.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';

import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import {
  ApplyCvRequest,
  ApplyCvResponse,
  ApplyCvWithExistingRequest,
  ApplyCvWithUploadRequest
} from '../../../../shared/models/jobs/apply-cv.model';
import { ResumeReviewInput } from '../../../../shared/models/jobs/resume-review-input.model';
import { ResumeReviewComponent } from '../../../../shared/components/resume-review/resume-review.component';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';

type CvMode = 'existing' | 'upload';
type ApplyCvFormGroup = FormGroup<{
  cvMode: FormControl<CvMode>;
  existingCvId: FormControl<number>;
  email: FormControl<string>;
  coverLetter: FormControl<string>;
}>;

@Component({
  selector: 'app-apply-cv',
  standalone: true,
  imports: [RouterModule, ReactiveFormsModule, ResumeReviewComponent, TranslatePipe],
  templateUrl: './apply-cv.component.html',
  styleUrl: './apply-cv.component.css'
})
export class ApplyCvComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  applyCvForm: ApplyCvFormGroup;
  jobId!: number;
  selectedFile: File | null = null;
  isDragging = false;
  readonly maxFileSize = 5 * 1024 * 1024;
  readonly acceptedExtensions = '.pdf,.doc,.docx';
  get previousCvOptions(): ResumeReviewInput[] {
    return this.resumeService.resumes$() || [];
  }

  constructor(private router: Router,
    private route: ActivatedRoute,
    private jobService: JobService,
    public resumeService: ResumeService,
    private notifyService: NotifyMessageService,
    private i18nService: I18nService,
  ) {
    this.applyCvForm = this.fb.nonNullable.group({
      cvMode: this.fb.nonNullable.control<CvMode>('existing', Validators.required),
      existingCvId: this.fb.nonNullable.control(this.previousCvOptions[0]?.id ?? 0, Validators.required),
      email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
      coverLetter: this.fb.nonNullable.control('', [Validators.maxLength(1000)])
    });

    this.updateExistingCvValidator('existing');

    effect(() => {
      const resumes = this.resumeService.resumes$();
      if (resumes && resumes.length > 0) {
        // If current value is 0 (default/unselected), select the first available resume
        if (this.applyCvForm.controls.existingCvId.value === 0) {
          this.applyCvForm.controls.existingCvId.setValue(resumes[0].id);
        }
      }
    });
  }

  ngOnInit(): void {
    this.resumeService.getUserResumes();
    this.route.params.pipe(take(1)).subscribe(params => {
      const parsedJobId = Number(params['id']);

      if (Number.isNaN(parsedJobId) || parsedJobId <= 0) {
        this.notifyService.error(this.i18nService.translate('applyCv.errors.invalidJobLink'));
        this.router.navigate(['/']);
        return;
      }

      this.jobId = parsedJobId;
      this.checkApplyJob();
    });
  }

  checkApplyJob(): void {
    this.jobService.checkApplyJob(this.jobId).pipe(take(1)).subscribe({
      next: (response) => {
        if (response.data) {
          this.notifyService.info(this.i18nService.translate('applyCv.notifications.alreadyApplied'));
          this.router.navigate(['/single', this.jobId]);
        }
      },
      error: (error: { status?: number }) => {
        if (error?.status === 401) {
          this.notifyService.warning(this.i18nService.translate('applyCv.errors.loginRequired'));
          this.router.navigate(['/login']);
        }
      }
    });
  }
  formatMoney(val: number): string {
    const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
    return `${val.toLocaleString(locale)} VND`;
  }

  get cvMode(): CvMode {
    return this.applyCvForm.get('cvMode')?.value ?? 'existing';
  }

  isMode(mode: 'existing' | 'upload'): boolean {
    return this.cvMode === mode;
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.setSelectedFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0] ?? null;
    this.setSelectedFile(file);
  }

  setCvMode(mode: 'existing' | 'upload'): void {
    this.applyCvForm.patchValue({ cvMode: mode });
    this.updateExistingCvValidator(mode);
    if (mode === 'existing') {
      this.selectedFile = null;
    }
  }

  private updateExistingCvValidator(mode: CvMode): void {
    const existingCvIdControl = this.applyCvForm.controls.existingCvId;
    if (mode === 'existing') {
      existingCvIdControl.setValidators([Validators.required]);
    } else {
      existingCvIdControl.clearValidators();
    }
    existingCvIdControl.updateValueAndValidity({ emitEvent: false });
  }

  private setSelectedFile(file: File | null): void {
    if (!file) {
      this.selectedFile = null;
      return;
    }

    const extension = `.${file.name.split('.').pop()?.toLowerCase() ?? ''}`;
    const isAllowedExtension = this.acceptedExtensions.includes(extension);
    const isAllowedSize = file.size <= this.maxFileSize;

    if (!isAllowedExtension || !isAllowedSize) {
      this.selectedFile = null;
      return;
    }

    this.selectedFile = file;
    this.setCvMode('upload');
  }

  get selectedFileSizeLabel(): string {
    if (!this.selectedFile) {
      return '';
    }

    const sizeInMb = this.selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }

  get selectedExistingCv(): ResumeReviewInput | null {
    const selectedId = this.applyCvForm.controls.existingCvId.value;
    return this.previousCvOptions.find(cv => cv.id === selectedId) ?? this.previousCvOptions[0] ?? null;
  }

  onSubmit(): void {
    if (this.applyCvForm.invalid) {
      this.applyCvForm.markAllAsTouched();
      this.notifyService.error(this.i18nService.translate('applyCv.errors.invalidForm'));
      return;
    }

    const payload = this.buildApplyCvPayload();
    if (!payload) {
      this.notifyService.error(this.i18nService.translate('applyCv.errors.cannotBuildPayload'));
      return;
    }

    if (this.isMode('existing')) {
      this.jobService.submitApplyCvExisting(payload as ApplyCvWithExistingRequest).subscribe({
        next: (response: ApplyCvResponse) => {
          this.notifyService.success(response.message || this.i18nService.translate('applyCv.notifications.success'));
          this.router.navigate(['/single', this.jobId]);
        },
        error: (error: any) => {
           this.notifyService.error(error.error?.message || this.i18nService.translate('applyCv.errors.submitFailed'));
        }
      });
      return;
    }

    this.jobService.submitApplyCvUpload(payload as ApplyCvWithUploadRequest).subscribe({
      next: (response: ApplyCvResponse) => {
        this.notifyService.success(response.message || this.i18nService.translate('applyCv.notifications.success'));
        this.router.navigate(['/single', this.jobId]);
      },
      error: (error: any) => {
        this.notifyService.error(error.error?.message || this.i18nService.translate('applyCv.errors.uploadFailed'));
      }
    });
  }

  private buildApplyCvPayload(): ApplyCvRequest | null {
    const formValue = this.applyCvForm.getRawValue();

    if (this.isMode('existing')) {
      const existingCvId = formValue.existingCvId;
      if (!existingCvId) {
        return null;
      }

      const payload: ApplyCvWithExistingRequest = {
        jobId: this.jobId,
        existingCvId,
        email: formValue.email,
        coverLetter: formValue.coverLetter
      };

      return payload;
    }

    if (!this.selectedFile) {
      return null;
    }

    const payload: ApplyCvWithUploadRequest = {
      jobId: this.jobId,
      cvFile: this.selectedFile,
      email: formValue.email,
      coverLetter: formValue.coverLetter
    };

    return payload;
  }
}
