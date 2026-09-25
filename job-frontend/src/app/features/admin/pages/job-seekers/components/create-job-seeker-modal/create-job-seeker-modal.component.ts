import {
  Component,
  Output,
  EventEmitter,
  HostListener,
  inject,
  effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import { AdminJobSeekersService } from '../../../../services/admin-job-seekers.service';

@Component({
  selector: 'app-create-job-seeker-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-job-seeker-modal.component.html',
})
export class CreateJobSeekerModalComponent {
  @Output() closeModal = new EventEmitter<void>();

  private readonly jobSeekersService = inject(AdminJobSeekersService);
  private readonly fb = inject(FormBuilder);

  readonly createJobSeekerForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    profession: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    resumeUrl: ['', [Validators.required, Validators.pattern(/^(https?:\/\/).+/i)]],
  });

  isCreating = false;
  createError: string | null = null;
  isConfirmDiscardOpen = false;

  constructor() {
    effect(() => {
      this.isCreating = this.jobSeekersService.isCreating();
    });
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    if (this.isConfirmDiscardOpen) {
      this.cancelDiscardChanges();
    } else {
      this.attemptCloseCreateDialog();
    }
  }

  attemptCloseCreateDialog(): void {
    if (this.createJobSeekerForm.dirty && !this.isCreating) {
      this.isConfirmDiscardOpen = true;
    } else {
      this.closeCreateDialog();
    }
  }

  confirmDiscardChanges(): void {
    this.isConfirmDiscardOpen = false;
    this.closeCreateDialog();
  }

  cancelDiscardChanges(): void {
    this.isConfirmDiscardOpen = false;
  }

  closeCreateDialog(): void {
    this.isConfirmDiscardOpen = false;
    this.createError = null;
    this.createJobSeekerForm.reset();
    this.closeModal.emit();
  }

  isCreateFieldInvalid(
    controlName: 'fullName' | 'email' | 'profession' | 'resumeUrl'
  ): boolean {
    const control = this.createJobSeekerForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  submitCreateJobSeeker(): void {
    if (this.createJobSeekerForm.invalid) {
      this.createJobSeekerForm.markAllAsTouched();
      return;
    }

    this.createError = null;
    const payload = this.createJobSeekerForm.getRawValue();

    this.jobSeekersService
      .createJobSeeker(payload)
      .pipe(take(1))
      .subscribe({
        next: () => {
          this.closeCreateDialog();
        },
        error: (err) => {
          this.createError =
            err?.error?.message ||
            'Không thể tạo hồ sơ người tìm việc. Vui lòng kiểm tra lại thông tin và thử lại.';
        },
      });
  }
}
