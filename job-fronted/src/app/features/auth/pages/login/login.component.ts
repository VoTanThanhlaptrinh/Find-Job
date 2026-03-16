import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import {concatMap, of, take} from 'rxjs';
import {MatIconModule} from '@angular/material/icon';
import {MatTab, MatTabChangeEvent, MatTabGroup} from '@angular/material/tabs';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-login',
  imports: [FormsModule, CommonModule, RouterLink, MatIconModule, MatTabGroup, MatTab],
  standalone: true,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
  googleUrl = '';
  loggedIn: boolean = false;
  username = '';
  password = '';
  role: string = 'ROLE_USER';
  constructor(private loginService: AuthService
    , private router: Router,
      private route: ActivatedRoute,
      private toastr: NotifyMessageService,
      private auth: AuthService) {
  }
  ngOnInit(): void {
    this.notify();
    this.googleLoginURL();
  }

  onLogin() {
    const loginRequest = {
      role: this.role,
      username: this.username,
      password: this.password
    };
    this.loginService.login(loginRequest).subscribe({
        next: (res: any) => {
          if(this.role === 'ROLE_USER')
            this.router.navigate(['/']);
          if(this.role === 'ROLE_HIRER')
            this.router.navigate(['/hirer']);
        },
        error: (error) => {
          this.toastr.showMessage(error || 'Lỗi đăng nhập','','error')
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
       console.log(error)
      }
    })
  }

  onTabClick(event: MatTabChangeEvent) {
    this.role = event.index === 0 ? 'ROLE_USER' : 'ROLE_HIRER';
  }
}
