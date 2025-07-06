import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../services/notify-message.service';
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
  role: string = 'USER';
  constructor(private loginService: AuthService
    , private router: Router,
      private route: ActivatedRoute,
      private toastr: NotifyMessageService,
      private auth: AuthService) {
  }
  ngOnInit(): void {
    this.notify();
    this.googleLoginURL();
    this.checkLogin();
  }
  checkLogin(){
    this.auth.checkUserLogin().pipe(
      concatMap(userOk => {
        if (userOk) {
          return of({ok: userOk});
        }
        return this.auth.checkHirerLogin().pipe(
          map(hirerOk => ({ok: hirerOk}))
        );
      })
    ).subscribe({
      next: value => {
        if(value.ok)
          this.router.navigate(['/']).then(() =>{
              if (typeof window !== 'undefined') {
                window.location.reload()
              }
          })
      },
      error: () => {
        this.router.navigate(['/login'])
      }
    });
  }


  onLogin() {
    const loginRequest = {
      role: this.role,
      username: this.username,
      password: this.password
    };
    this.loginService.login(loginRequest).subscribe({
        next: (res) => {
          this.router.navigate(['/']);
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
    this.role = event.index === 0 ? 'USER' : 'HIRER';
  }
}
