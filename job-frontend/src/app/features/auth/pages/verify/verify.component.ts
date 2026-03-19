import { Component, OnInit } from '@angular/core';
import { AccountService } from '../../../../core/services/account.service';
import { ActivatedRoute, Router } from '@angular/router';
import {NotifyMessageService} from '../../../../core/services/notify-message.service';

@Component({
  selector: 'app-verify',
  imports: [],
  templateUrl: './verify.component.html',
  styleUrl: './verify.component.css'
})
export class VerifyComponent implements OnInit {
  constructor(private accountService: AccountService
              , private aRouter: ActivatedRoute
              , private router: Router
              ,private notify: NotifyMessageService) { }
  email: string = '';

  ngOnInit(): void {
    this.aRouter.queryParams.subscribe(params => {
      this.email = params['email'];
    });
  }

  sendLink(email: string) {
    this.accountService.sendLink(email).subscribe(
      {
        next: (res) => {
          this.notify.showMessage("Gửi thành công",'success');
        },
        error: (err) => {
          this.notify.showMessage(err?.message,'error');
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

