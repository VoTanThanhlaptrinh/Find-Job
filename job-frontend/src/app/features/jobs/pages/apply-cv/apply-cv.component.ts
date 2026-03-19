import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { JobServiceService } from '../../services/job-service.service';

import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import {
  ApplyCvRequest,
  ApplyCvResponse,
  ApplyCvWithExistingRequest,
  ApplyCvWithUploadRequest
} from '../../../../shared/models/jobs/apply-cv.model';

type CvMode = 'existing' | 'upload';
type ApplyCvFormGroup = FormGroup<{
  cvMode: FormControl<CvMode>;
  existingCvId: FormControl<number>;
  email: FormControl<string>;
  coverLetter: FormControl<string>;
}>;

interface PreviousCvOption {
  id: number;
  label: string;
}

@Component({
  selector: 'app-apply-cv',
  imports: [RouterModule, ReactiveFormsModule],
  templateUrl: './apply-cv.component.html',
  styleUrl: './apply-cv.component.css'
})
export class ApplyCvComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  applyCvForm: ApplyCvFormGroup;
  jobDetail: any;
  jobId!: number;
  selectedFile: File | null = null;
  isDragging = false;
  readonly maxFileSize = 5 * 1024 * 1024;
  readonly acceptedExtensions = '.pdf,.doc,.docx';
  readonly previousCvOptions: PreviousCvOption[] = [
    { id: 1, label: 'CV Backend Developer - Applied at Frontend Developer' },
    { id: 2, label: 'CV Fullstack Developer - Applied at Software Engineer' }
  ];

  constructor(private router: Router,
    private route: ActivatedRoute,
    private jobService: JobServiceService
  ) {
    this.applyCvForm = this.fb.nonNullable.group({
      cvMode: this.fb.nonNullable.control<CvMode>('existing', Validators.required),
      existingCvId: this.fb.nonNullable.control(this.previousCvOptions[0]?.id ?? 0, Validators.required),
      email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
      coverLetter: this.fb.nonNullable.control('')
    });

    this.updateExistingCvValidator('existing');
  }

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe(params => {
      this.jobId = params['id'];
    });
  }
  checkApplyJob() {
    this.jobService.checkApplyJob(this.jobId).pipe(take(1)).subscribe({
      next: (response: any) => {
        if (!response.data) {
          this.router.navigate(['/']);
        }
      }
    });
  }
  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
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

  onSubmit(): void {
    if (this.applyCvForm.invalid) {
      this.applyCvForm.markAllAsTouched();
      return;
    }

    const payload = this.buildApplyCvPayload();
    if (!payload) {
      return;
    }

    if (this.isMode('existing')) {
      this.jobService.submitApplyCvExisting(payload as ApplyCvWithExistingRequest).subscribe({
        next: (response: ApplyCvResponse) => {
          console.log('Submit existing CV success:', response);
        },
        error: (error: unknown) => {
          console.error('Submit existing CV failed:', error);
        }
      });
      return;
    }

    this.jobService.submitApplyCvUpload(payload as ApplyCvWithUploadRequest).subscribe({
      next: (response: ApplyCvResponse) => {
        console.log('Submit upload CV success:', response);
      },
      error: (error: unknown) => {
        console.error('Submit upload CV failed:', error);
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
