import { Component, effect, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { RecruiterAuthService } from '../../services/recruiter-auth.service';

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
  private readonly route = inject(ActivatedRoute);
  private readonly recruiterAuth = inject(RecruiterAuthService);
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly defaultSuccessUrl = '/recruiter/dashboard';

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    remember: [false],
  });

  showPassword = false;
  isSubmitting = false;
  formError: string | null = null;

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  constructor() {
    effect(() => {
      this.isSubmitting = this.recruiterAuth.isSubmittingAuth$();
      this.formError = this.recruiterAuth.authError$();
    });

    const existingToken = this.tokenService.getToken();

    console.info('[RecruiterLogin] Initializing recruiter login page', {
      currentUrl: this.router.url,
      returnUrl: this.route.snapshot.queryParamMap.get('returnUrl'),
      existingRoles: this.tokenService.getTokenRoles(existingToken),
      hasExistingToken: !!existingToken,
    });

    if (this.isHirerToken(existingToken)) {
      this.authService.setLoggedIn(true);
      this.navigateAfterLogin('existing-token');
    }

    effect(() => {
      const token = this.recruiterAuth.loginToken$();
      if (!token) {
        return;
      }

      this.tokenService.setToken(token);
      this.authService.setLoggedIn(true);

      console.info('[RecruiterLogin] Recruiter login token received', {
        currentUrl: this.router.url,
        returnUrl: this.route.snapshot.queryParamMap.get('returnUrl'),
        roles: this.tokenService.getTokenRoles(token),
      });

      if (this.isHirerToken(token)) {
        this.navigateAfterLogin('login-success');
        return;
      }

      this.tokenService.clearToken();
      this.authService.setLoggedIn(false);
      console.warn('[RecruiterLogin] Login token does not contain recruiter role. Clearing token.');
    });
  }

  private isHirerToken(token: string): boolean {
    return this.tokenService.hasAnyRole(['HIRER', 'ROLE_HIRER'], token);
  }



  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      console.warn('[RecruiterLogin] Submit blocked because the form is invalid.', {
        emailErrors: this.form.controls.email.errors,
        passwordErrors: this.form.controls.password.errors,
      });
      return;
    }

    this.recruiterAuth.clearAuthError();

    const { email, password } = this.form.getRawValue();
    console.info('[RecruiterLogin] Submitting recruiter login request', {
      email,
      currentUrl: this.router.url,
      returnUrl: this.route.snapshot.queryParamMap.get('returnUrl'),
    });
    this.recruiterAuth.login({ username: email, password });
  }

  private navigateAfterLogin(source: string): void {
    const targetUrl = this.resolveSuccessUrl();
    console.info('[RecruiterLogin] Navigating after recruiter authentication', {
      source,
      currentUrl: this.router.url,
      targetUrl,
    });
    void this.router.navigateByUrl(targetUrl);
  }

  private resolveSuccessUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl && returnUrl.startsWith('/recruiter/')) {
      return returnUrl;
    }

    return this.defaultSuccessUrl;
  }
}
