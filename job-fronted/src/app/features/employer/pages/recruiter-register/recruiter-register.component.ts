import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-recruiter-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './recruiter-register.component.html',
  styleUrl: './recruiter-register.component.css',
})
export class RecruiterRegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    companyName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern('^0[0-9]{9,10}$')]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
    agreeTerms: [false, [Validators.requiredTrue]],
  });

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

    this.router.navigate(['/recruiter/dashboard']);
  }
}
