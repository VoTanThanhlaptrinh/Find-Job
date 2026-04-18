import { Component, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RecruiterAuthService } from '../../services/recruiter-auth.service';
import { TokenService } from '../../../../core/services/token.service';

@Component({
  selector: 'app-recruiter-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recruiter-login.component.html',
  styleUrl: './recruiter-login.component.css',
})
export class RecruiterLoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly recruiterAuth = inject(RecruiterAuthService);
  private readonly tokenService = inject(TokenService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
  });

  constructor() {
    const existingToken = this.tokenService.getToken();
    if (this.isHirerToken(existingToken)) {
      this.router.navigate(['/recruiter/dashboard']);
    }

    effect(() => {
      const token = this.recruiterAuth.loginToken$();
      if (!token) {
        return;
      }

      this.tokenService.setToken(token);
      if (this.isHirerToken(token)) {
        this.router.navigate(['/recruiter/dashboard']);
        return;
      }

      this.tokenService.clearToken();
    });
  }

  private isHirerToken(token: string): boolean {
    return this.tokenService.hasAnyRole(['HIRER', 'ROLE_HIRER'], token);
  }

  get isSubmitting(): boolean {
    return this.recruiterAuth.isSubmittingAuth$();
  }

  get formError(): string | null {
    return this.recruiterAuth.authError$();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.recruiterAuth.clearAuthError();

    const { email, password } = this.form.getRawValue();
    this.recruiterAuth.login({ username: email, password });
  }
}
