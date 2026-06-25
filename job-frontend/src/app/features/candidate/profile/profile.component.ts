import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { effect } from '@angular/core';
import { take } from 'rxjs';
import { UserService } from '../../../core/services/user.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

interface UserUi {
  fullName: string;
  address: string;
  dateOfBirth: string;
  mobile: string;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatNativeDateModule,
    MatDatepickerToggle,
    MatDatepicker,
    MatDatepickerInput,
    TranslatePipe
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly toastr = inject(NotifyMessageService);
  private readonly i18n = inject(I18nService);

  readonly formGroup = new FormGroup({
    fullName: new FormControl('', Validators.required),
    address: new FormControl('', Validators.required),
    mobile: new FormControl('', [Validators.required, Validators.minLength(10)]),
    dateOfBirth: new FormControl<Date | null>(null, Validators.required)
  });

  user: UserUi = {
    fullName: '',
    address: '',
    dateOfBirth: '',
    mobile: ''
  };

  constructor() {
    effect(() => {
      const userDetails = this.userService.userDetails();
      if (userDetails) {
        this.user = userDetails;
        this.formGroup.patchValue({
          fullName: this.user.fullName,
          address: this.user.address,
          mobile: this.user.mobile,
          dateOfBirth: this.user.dateOfBirth ? new Date(this.user.dateOfBirth) : null
        });
      }
    });
  }

  ngOnInit(): void {
    this.userService.getDetails();
  }

  onSubmit(): void {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }

    this.userService.updateInfo(this.formGroup.value).subscribe({
      next: (res) => {
        this.toastr.showMessage(this.i18n.translate('profile.success'), '', 'success');
      },
      error: (err) => {
        this.toastr.showMessage(err?.error?.message || this.i18n.translate('profile.error'), '', 'error');
      }
    });
  }
}
