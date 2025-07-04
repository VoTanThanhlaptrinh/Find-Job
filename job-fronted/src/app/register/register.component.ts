import {Component, OnInit} from '@angular/core';
import { AuthService } from '../services/auth.service';
import { FormBuilder, FormGroup, FormsModule, MinLengthValidator, PatternValidator, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ErrorFieldComponent } from '../error-field/error-field.component';
import {MatTab, MatTabChangeEvent, MatTabGroup} from '@angular/material/tabs';

@Component({
  selector: 'app-register',
  imports: [FormsModule, CommonModule, ErrorFieldComponent, ReactiveFormsModule, MatTabGroup, MatTab],
  standalone: true,
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent implements OnInit{
  googleUrl = '';
  registerForm: FormGroup;
  formErrors = null;
  role: string = 'user';
  constructor(private auth: AuthService, private router: Router, private fb: FormBuilder) {
    this.registerForm = this.fb.group({
      role: ['', Validators.required],
      fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(254)]],
      username: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.auth.getGoogleLoginUrl().subscribe(r => this.googleUrl = r.authURL)
  }
  validationMessages = {
    fullName: {
      required: 'Họ và tên không được để trống',
      minlength: 'Họ và tên phải có ít nhất 3 ký tự',
    },
    email: {
      required: 'Email không được để trống',
      email: 'Vui lòng nhập địa chỉ email hợp lệ',
    },
    password: {
      required: 'Mật khẩu không được để trống',
      minlength: 'Mật khẩu phải có ít nhất 8 ký tự',
    },
    confirmPassword: {
      required: 'Xác nhận mật khẩu không được để trống',
      minlength: 'Xác nhận mật khẩu phải có ít nhất 8 ký tự',
      mismatch: 'Mật khẩu và xác nhận mật khẩu không khớp',
    },
  };
  passwordMatchValidator(form: FormGroup) {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (password && confirmPassword) {
      if (password.value !== confirmPassword.value) {
        confirmPassword.setErrors({ mismatch: true });
      } else {
        if (confirmPassword.hasError('mismatch')) {
          confirmPassword.setErrors(null);
        }
      }
    }
    return null;
  }
  onRegister() {
    this.formErrors = null; // reset lỗi
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched(); // show ra hết lỗi
      return;
    }
    this.registerForm.patchValue({
      role : this.role
    })
    this.auth.register(this.registerForm.value).subscribe({
      next: res => {
        this.router.navigate(['/verify'], { queryParams: { email: res.email}});
      }, error: (error) => {
        this.formErrors = error;
      }
    });
  }

  onTabChanged($event: MatTabChangeEvent) {
    this.role = ($event.index === 0 ? 'USER' : 'HIRER')
  }
}
