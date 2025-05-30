import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { on } from 'node:events';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-verify',
  imports: [],
  templateUrl: './verify.component.html',
  styleUrl: './verify.component.css'
})
export class VerifyComponent implements OnInit {
  constructor(private authService: AuthService, private aRouter: ActivatedRoute, private router: Router) { }
  email: string = '';
  ngOnInit(): void {
    this.aRouter.queryParams.subscribe(params => {
      this.email = params['email'];
      console.log(this.email);
      if (this.email) {
        this.sendLink(this.email);
      } else {
        this.router.navigate(['/login']);
      }
    });
  }

  sendLink(email: string) {
    this.authService.sendLink(email).subscribe(
      {
        next: (res) => {
          console.info('thanh cong', res);
        },
        error: (err) => {
          console.error('that bai', err);

        }
      }
    );
  }
  reSendLink() {
    if (this.email) {
      this.sendLink(this.email);
    }
  }
}

