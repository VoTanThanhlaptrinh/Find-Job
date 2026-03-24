import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AccountService } from '../../../../core/services/account.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';

@Component({
  selector: 'app-change-pass',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './change-pass.component.html',
  styleUrl: './change-pass.component.css',
  standalone: true
})
export class ChangePassComponent implements OnInit {
  requiresVerification = false;

  constructor(
    private readonly accountService: AccountService,
    private readonly notify: NotifyMessageService
  ) {}

  readonly formGroup = new FormGroup({
    oldPass: new FormControl('', [Validators.required, Validators.minLength(6)]),
    newPass: new FormControl('', [Validators.required, Validators.minLength(6)]),
    confirmPass: new FormControl('', [Validators.required, Validators.minLength(6)])
  });

  ngOnInit(): void {
    this.accountService.checkOauth2().subscribe({
      next: (res) => {
        this.requiresVerification = !res;
      }
    });
  }

  isMatch(): boolean {
    return this.formGroup.value.confirmPass === this.formGroup.value.newPass;
  }

  onSubmit(): void {
    if (this.requiresVerification) {
      return;
    }

    if (!this.isMatch()) {
      this.notify.showMessage('Xác nhận mật khẩu phải trùng với mật khẩu mới.', '', 'error');
      return;
    }

    this.accountService.changePass(this.formGroup.value).subscribe({
      next: () => {
        this.notify.showMessage('Thay đổi mật khẩu thành công.', '', 'success');
        this.formGroup.reset();
      },
      error: (err) => {
        this.notify.showMessage(err?.error?.message || 'Có lỗi xảy ra.', '', 'error');
      }
    });
  }
}
