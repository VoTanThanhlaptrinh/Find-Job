import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import {AuthService} from '../services/auth.service';
import {take} from 'rxjs';
import {NotifyMessageService} from '../services/notify-message.service';
import {MatDatepicker, MatDatepickerInput, MatDatepickerToggle} from '@angular/material/datepicker';
import {MatFormField, MatHint, MatInput, MatSuffix} from '@angular/material/input';
import {MatNativeDateModule} from '@angular/material/core';

  interface userUI{
    fullName: string,
    address: string,
    dateOfBirth: string,
    mobile: string,
  }
@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule, ReactiveFormsModule,MatNativeDateModule, MatSuffix, MatDatepickerToggle, MatDatepicker, MatSuffix, ReactiveFormsModule, MatDatepickerInput, MatFormField, MatInput],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit{
    formGroup= new  FormGroup({
      fullName: new FormControl('', Validators.required),
      address: new FormControl('', Validators.required),
      mobile: new FormControl('', [Validators.required, Validators.minLength(10)]),
      dateOfBirth: new FormControl<Date | null>(null, Validators.required)
    })
  constructor(private auth: AuthService
              ,private toastr: NotifyMessageService ) {
  }
  ngOnInit(): void {
     this.getDetails();
  }
  user:userUI = {
    fullName: '',
    address: '',
    dateOfBirth: '',
    mobile: '',
  };
  getDetails(){
    this.auth.getDetails().pipe(take(1)).subscribe({
      next: (res) =>{
       this.user = res.data;
       this.formGroup.patchValue({
         fullName: this.user.fullName,
         address: this.user.address,
         mobile: this.user.mobile,
         dateOfBirth: this.user.dateOfBirth ? new Date(this.user.dateOfBirth) : null
       })
      },
      error:(err) =>{
        this.toastr.showMessage('Có lỗi xảy ra!','','error')
        this.auth.logout();
      }
    })
  }
  onSubmit(){
    if(this.formGroup.invalid){
      this.formGroup.markAllAsTouched();
      return;
    }
    this.auth.updateInfo(this.formGroup.value).subscribe({
      next: res =>{
        this.toastr.showMessage(res.message,'','success')
      },error: err =>{
        this.toastr.showMessage(err?.error?.message || 'Có lỗi xảy ra','','error')
      }
    })
  }
}
