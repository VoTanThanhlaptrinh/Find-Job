import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CommonModule } from '@angular/common';
import { AccountService } from '../../../core/services/account.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-change-pass',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, TranslatePipe],
  templateUrl: './change-pass.component.html',
  styleUrl: './change-pass.component.css',
  standalone: true
})
export class ChangePassComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly notify = inject(NotifyMessageService);
  private readonly i18n = inject(I18nService);

  requiresVerification = false;
  showOldPass = signal(false);
  showNewPass = signal(false);
  showConfirmPass = signal(false);

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

  toggleOldPass() { this.showOldPass.update(v => !v); }
  toggleNewPass() { this.showNewPass.update(v => !v); }
  toggleConfirmPass() { this.showConfirmPass.update(v => !v); }

  onSubmit(): void {
    if (this.requiresVerification) {
      return;
    }

    if (!this.isMatch()) {
      this.notify.showMessage(this.i18n.translate('changePassword.matchError'), '', 'error');
      return;
    }

    this.accountService.changePass(this.formGroup.value).subscribe({
      next: () => {
        this.notify.showMessage(this.i18n.translate('changePassword.successMessage'), '', 'success');
        this.formGroup.reset();
      },
      error: (err) => {
        this.notify.showMessage(err?.error?.message || 'Error occurred.', '', 'error');
      }
    });
  }
}
