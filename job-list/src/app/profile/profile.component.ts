import {Component, OnInit} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {AuthService} from '../services/auth.service';
import {take} from 'rxjs';
import {NotifyMessageService} from '../services/notify-message.service';

  interface userUI{
    fullName: string,
    birthMonth: string,
    birthDay: string,
    birthYear: string,
    address: string,
    mobile: string,
  }
  interface backendUser{
    fullName: string,
    dateOfBirth: string,
    address: string,
    mobile: string,
  }
@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit{
  constructor(private auth: AuthService
              ,private toastr: NotifyMessageService ) {
  }
  ngOnInit(): void {
     this.getDetails()
  }
  user:userUI = {
    fullName: '',
    birthMonth: '',
    birthDay: '',
    birthYear: '',
    address: '',
    mobile: '',
  };
  private mappingToUserUI(data: backendUser): userUI{
    let birthYear = '', birthMonth = '', birthDay = '';
    if(data.dateOfBirth){
      const[year,month,day] = data.dateOfBirth.split('-');
      birthYear = year;
      birthMonth = month;
      birthDay = day;
    }
    return {
      fullName: data.fullName,
      birthYear,
      birthMonth,
      birthDay,
      address: data.address,
      mobile: data.mobile,
    }
  }
  getDetails(){
    this.auth.getDetails().pipe(take(1)).subscribe({
      next: (res) =>{
       this.user = this.mappingToUserUI(res.data);
      },
      error:(err) =>{
        console.error(err)
        this.toastr.showMessage('Có lỗi xảy ra!','','error')
      }
    })
  }
}
