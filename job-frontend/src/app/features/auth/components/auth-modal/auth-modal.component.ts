import { Component, Inject, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors, ValidatorFn, FormGroup } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AccountService } from '../../../../core/services/account.service';

export interface AuthModalData {
  initialTab: 'login' | 'register';
}

@Component({
  selector: 'app-auth-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatTabsModule],
  templateUrl: './auth-modal.component.html',
  styleUrl: './auth-modal.component.css'
})
export class AuthModalComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  selectedTabIndex = 0;
  googleUrl = '';

  // Login State
  isLoginSubmitting = false;
  showLoginPassword = false;

  // Register State
  formErrors: string | null = null;
  isRegisterSubmitting = false;
  showRegisterPassword = false;
  showConfirmPassword = false;

  // Forms
  readonly loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, this.usernameOrEmailValidator]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

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

  constructor(
    public dialogRef: MatDialogRef<AuthModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AuthModalData,
    private auth: AuthService,
    private accountService: AccountService,
    private router: Router
  ) {
    this.selectedTabIndex = data?.initialTab === 'register' ? 1 : 0;
  }

  ngOnInit(): void {
    this.accountService.getGoogleLoginUrl().subscribe((r: any) => {
      this.googleUrl = r?.data || r?.authURL || '';
    });
  }

  // --- Login Logic ---
  get loginUsernameControl() { return this.loginForm.controls.username; }
  get loginPasswordControl() { return this.loginForm.controls.password; }

  toggleLoginPassword() {
    this.showLoginPassword = !this.showLoginPassword;
  }

  private usernameOrEmailValidator(control: AbstractControl<string>): ValidationErrors | null {
    const value = (control.value || '').trim();
    if (!value) return null;
    const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    const isUsername = /^[a-zA-Z0-9._-]{4,30}$/.test(value);
    return isEmail || isUsername ? null : { usernameOrEmail: true };
  }

  onLogin() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }
    this.isLoginSubmitting = true;
    const { username, password } = this.loginForm.getRawValue();
    this.auth.login({ username, password });

    setTimeout(() => {
      this.isLoginSubmitting = false;
    }, 2000);
  }

  // --- Register Logic ---
  get regFullNameControl() { return this.registerForm.controls.fullName; }
  get regUsernameControl() { return this.registerForm.controls.username; }
  get regPasswordControl() { return this.registerForm.controls.password; }
  get regConfirmPasswordControl() { return this.registerForm.controls.confirmPassword; }

  toggleRegisterPassword() { this.showRegisterPassword = !this.showRegisterPassword; }
  toggleConfirmPassword() { this.showConfirmPassword = !this.showConfirmPassword; }

  get passwordStrength() {
    const pwd = this.regPasswordControl.value || '';
    if (!pwd) return { level: 'none', percent: 0, label: '', colorClass: 'bg-slate-200' };

    let score = 0;
    if (pwd.length >= 8) score += 1;
    if (/[a-z]/.test(pwd)) score += 1;
    if (/[A-Z]/.test(pwd)) score += 1;
    if (/\d/.test(pwd)) score += 1;
    if (/[^\w\s]/.test(pwd)) score += 1;

    if (score <= 2) return { level: 'weak', percent: 25, label: 'Yếu', colorClass: 'bg-red-500' };
    if (score === 3) return { level: 'medium', percent: 50, label: 'Trung bình', colorClass: 'bg-orange-500' };
    if (score === 4) return { level: 'strong', percent: 75, label: 'Mạnh', colorClass: 'bg-emerald-500' };
    return { level: 'very-strong', percent: 100, label: 'Rất mạnh', colorClass: 'bg-blue-500' };
  }

  passwordMatchValidator(form: FormGroup): ValidationErrors | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (!password || !confirmPassword) return null;

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

  shouldShowError(control: AbstractControl | null): boolean {
    if (!control) return false;
    return control.invalid && (control.touched || control.dirty);
  }

  onRegister() {
    this.formErrors = null;
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }
    this.isRegisterSubmitting = true;
    const payload = this.registerForm.getRawValue();

    this.auth.register(payload).subscribe({
      next: (res: any) => {
        this.dialogRef.close(true);
        this.router.navigate(['/verify'], { queryParams: { email: res.email } });
      },
      error: (error: any) => {
        this.formErrors = error;
        this.isRegisterSubmitting = false;
      },
      complete: () => {
        this.isRegisterSubmitting = false;
      },
    });
  }

  switchToLogin() {
    this.selectedTabIndex = 0;
  }

  switchToRegister() {
    this.selectedTabIndex = 1;
  }
}
