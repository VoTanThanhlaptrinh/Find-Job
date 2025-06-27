import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../services/notify-message.service';
import { take } from 'rxjs';
import {response} from 'express';
@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule],
  standalone: true,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  googleUrl = '';
  constructor(private loginService: AuthService
    , private router: Router,
      private route: ActivatedRoute,
      private toastr: NotifyMessageService,
      private auth: AuthService) {
  }
  ngOnInit(): void {
    this.notify()

    this.googleLoginURL()
  }
  formErrors: boolean = false;
  formErrorMessage: string = '';
  username = '';
  password = '';
  onLogin() {
    this.loginService.login(this.username, this.password).pipe(take(1)).subscribe({
        next: (response) => {
          let jwtToken = response.data;
          if (jwtToken) {
            localStorage.setItem('jwtToken', jwtToken);
            this.router.navigate([''])
          }
        },
        error: (error) => {
          this.formErrors = true;
          this.formErrorMessage = error.error.message;
        }
      }
    )
  }
  notify() {
    this.route.queryParams.pipe(take(1)).subscribe(params => {
      this.toastr.showMessage(params['message'],'' ,params['status'])
    })
  }
  googleLoginURL():void{
    this.auth.getGoogleLoginUrl().pipe(take(1)).subscribe({
      next: (response) => {
        this.googleUrl = response.data
      },
      error: (error) => {
        this.formErrorMessage = error.error.message;
      }
    })
  }
}
