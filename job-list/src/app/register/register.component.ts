import { Component } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { FormBuilder, FormGroup, FormsModule, MinLengthValidator, PatternValidator, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ErrorFieldComponent } from '../error-field/error-field.component';
import { table } from 'node:console';
import { take } from 'rxjs';

@Component({
  selector: 'app-register',
  imports: [FormsModule, CommonModule, ErrorFieldComponent, ReactiveFormsModule],
  standalone: true,
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  registerForm: FormGroup;
  formErrors = null;
  constructor(private auth: AuthService, private router: Router, private fb: FormBuilder) {
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(254)]],
      username: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
    }, { validators: this.passwordMatchValidator });
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
        // Xóa lỗi 'mismatch' nếu khớp
        if (confirmPassword.hasError('mismatch')) {
          confirmPassword.setErrors(null);
        }
      }
    }
    return null; // luôn trả về null cho FormGroup
  }
  
  onRegister() {
    this.formErrors = null; // reset lỗi
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched(); // show ra hết lỗi
      return;
    }
    this.auth.register(this.registerForm.value).pipe(take(1)).subscribe({
      next: (response) => {
        if(response.status === 200) {
            this.router.navigate(['/verify'], { queryParams: { email: response.data } });
        }
      }, error: (error) => {
        this.formErrors = error.error.message;
      }
    });
  }
}
