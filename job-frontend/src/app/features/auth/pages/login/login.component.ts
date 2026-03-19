import { Component, inject, OnInit } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { AccountService } from '../../../../core/services/account.service';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';


@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  standalone: true,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  googleUrl = '';
  isSubmitting = false;

  readonly loginForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, this.usernameOrEmailValidator]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor(
    private loginService: AuthService,
    private accountService: AccountService,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: NotifyMessageService
  ) {
  }

  ngOnInit(): void {
    this.googleLoginURL();
  }

  onLogin() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const { username, password } = this.loginForm.getRawValue();
    const loginRequest = {
      username,
      password,
    };
    this.loginService.login(loginRequest);
  }

  googleLoginURL(): void {
    this.accountService.getGoogleLoginUrl().subscribe({
      next: (response) => {
        this.googleUrl = response.data;
      }
    });
  }

  get usernameControl() {
    return this.loginForm.controls.username;
  }

  get passwordControl() {
    return this.loginForm.controls.password;
  }

  private usernameOrEmailValidator(
    control: AbstractControl<string>
  ): ValidationErrors | null {
    const value = (control.value || '').trim();
    if (!value) {
      return null;
    }

    const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    const isUsername = /^[a-zA-Z0-9._-]{4,30}$/.test(value);

    return isEmail || isUsername ? null : { usernameOrEmail: true };
  }
}
