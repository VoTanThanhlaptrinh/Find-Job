import { Component, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RecruiterAuthService } from '../../services/recruiter-auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-recruiter-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recruiter-register.component.html',
  styleUrl: './recruiter-register.component.css',
})
export class RecruiterRegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly recruiterAuth = inject(RecruiterAuthService);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    companyName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern('^0[0-9]{9,10}$')]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
    agreeTerms: [false, [Validators.requiredTrue]],
  });

  showPassword = false;
  showConfirmPassword = false;
  isSubmitting = false;
  formError: string | null = null;

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  constructor() {
    effect(() => {
      this.isSubmitting = this.recruiterAuth.isSubmittingAuth$();
      this.formError = this.recruiterAuth.authError$();

      const isRegistered = this.recruiterAuth.registerSuccess$();
      if (!isRegistered) {
        return;
      }

      this.recruiterAuth.resetRegisterState();
      this.router.navigate(['/recruiter/login']);
    });
  }



  get isPasswordMismatch(): boolean {
    const password = this.form.controls.password.value;
    const confirmPassword = this.form.controls.confirmPassword.value;
    return !!password && !!confirmPassword && password !== confirmPassword;
  }

  onSubmit(): void {
    if (this.form.invalid || this.isPasswordMismatch) {
      this.form.markAllAsTouched();
      return;
    }

    this.recruiterAuth.clearAuthError();

    const { fullName, companyName, email, phone, password } = this.form.getRawValue();

    this.recruiterAuth.register({
      fullName,
      companyName,
      email,
      phone,
      password
    });
  }
}
