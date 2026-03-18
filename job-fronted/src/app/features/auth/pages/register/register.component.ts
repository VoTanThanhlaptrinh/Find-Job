import { Component, inject, OnInit } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  standalone: true,
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  googleUrl = '';
  formErrors: string | null = null;
  isSubmitting = false;

  readonly registerForm = this.fb.nonNullable.group(
    {
      fullName: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(254),
          Validators.pattern(/^[\p{L}\s'.-]+$/u),
        ],
      ],
      username: ['', [Validators.required, Validators.email]],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(64),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,64}$/),
        ],
      ],
      confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
    },
    { 
    validators: [this.passwordMatchValidator as ValidatorFn] 
  }
  );

  constructor(private auth: AuthService, private router: Router) {
  }

  ngOnInit(): void {
    this.auth.getGoogleLoginUrl().subscribe((r) => {
      this.googleUrl = r?.data || r?.authURL || '';
    });
  }

  get fullNameControl() {
    return this.registerForm.controls.fullName;
  }

  get usernameControl() {
    return this.registerForm.controls.username;
  }

  get passwordControl() {
    return this.registerForm.controls.password;
  }

  get confirmPasswordControl() {
    return this.registerForm.controls.confirmPassword;
  }

  passwordMatchValidator(form: FormGroup): ValidationErrors | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    if (password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ ...(confirmPassword.errors || {}), mismatch: true });
      return { mismatch: true };
    }

    if (confirmPassword.hasError('mismatch')) {
      const errors = { ...(confirmPassword.errors || {}) };
      delete errors['mismatch'];
      const hasOtherErrors = Object.keys(errors).length > 0;
      confirmPassword.setErrors(hasOtherErrors ? errors : null);
    }

    return null;
  }

  onRegister() {
    this.formErrors = null;

    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const payload = this.registerForm.getRawValue();

    this.auth.register(payload).subscribe({
      next: (res) => {
        this.router.navigate(['/verify'], { queryParams: { email: res.email } });
      },
      error: (error) => {
        this.formErrors = error;
        this.isSubmitting = false;
      },
      complete: () => {
        this.isSubmitting = false;
      },
    });
  }

  shouldShowError(control: AbstractControl | null): boolean {
    if (!control) {
      return false;
    }
    return control.invalid && (control.touched || control.dirty);
  }
}
