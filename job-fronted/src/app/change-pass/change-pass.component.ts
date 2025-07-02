import { Component } from '@angular/core';
import {AuthService} from '../services/auth.service';
import {NotifyMessageService} from '../services/notify-message.service';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';

@Component({
  selector: 'app-change-pass',
  imports: [ReactiveFormsModule],
  templateUrl: './change-pass.component.html',
  styleUrl: './change-pass.component.css',
  standalone:true
})
export class ChangePassComponent {
  constructor(private authService: AuthService,
              private notify: NotifyMessageService) {
  }
  formGroup = new FormGroup({
    oldPass: new FormControl('', [Validators.required, Validators.minLength(6)]),
    newPass: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPass : new FormControl('', [Validators.required, Validators.minLength(6)]),
  })
  isMatch(): boolean{
    return this.formGroup.value.confirmPass === this.formGroup.value.newPass
  }
  onSubmit(){
    if(!this.isMatch()){
      this.notify.showMessage('Xác nhận mật khẩu phải trùng với mật khẩu mới','','error');
      return;
    }
    console.log('Form value:', this.formGroup.value);
    console.log('Form valid:', this.formGroup.valid);

    Object.keys(this.formGroup.controls).forEach(key => {
      const control = this.formGroup.get(key);
      console.log(`${key} - valid: ${control?.valid}, errors:`, control?.errors);
    });

    // if(this.formGroup.invalid){
    //   this.formGroup.markAllAsTouched()
    //   return
    // }
    console.log('abc')
    this.authService.changePass(this.formGroup.value).subscribe({
      next: res =>{
        this.notify.showMessage('Thay đổi mật khẩu thành công');
        this.formGroup.reset();
      },error: err =>{
        this.notify.showMessage(err?.error?.message || 'Có lỗi xảy ra','','error')
      }
    })
  }
}
