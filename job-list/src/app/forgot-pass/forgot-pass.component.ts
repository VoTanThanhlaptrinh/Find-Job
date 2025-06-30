import { Component } from '@angular/core';
import {FormControl, FormControlName, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {CommonModule} from '@angular/common';

@Component({
  selector: 'app-forgot-pass',
  imports: [
    FormsModule,
    RouterLink,
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './forgot-pass.component.html',
  styleUrl: './forgot-pass.component.css'
})
export class ForgotPassComponent {
    isError : boolean = false
    message: string = ''
    randomVerify: string | undefined = undefined
    formGroup = new FormGroup({
      email: new FormControl('', Validators.required),
      code: new FormControl('',Validators.required),
    })
  constructor(private authService: AuthService
              ,private router: Router) {
  }
  onSubmit(){
    if(this.formGroup.invalid){
      this.formGroup.markAllAsTouched();
      return;
    }
      this.authService.forgotPass(this.formGroup.value).subscribe({
        next: res =>{
            this.isError = false
            this.randomVerify = res.data
            this.router.navigate(['/reset-pass',this.randomVerify])
        },error: err => {
          this.isError = true
          this.message = err.message
        }
      });
  }
  getCode(){
      let email = this.formGroup.value.email
      if(email === undefined || email === null || email === ''){
        this.message = 'Nhập email để lấy mã xác thực'
        this.isError = true
      }else{
        this.authService.sendCode(email).subscribe({
          next: res =>{
            this.message = res.message
            this.isError = false
          },error: err => {
            this.isError = true
            this.message = err?.error?.message
          }
        })
      }
  }
}
