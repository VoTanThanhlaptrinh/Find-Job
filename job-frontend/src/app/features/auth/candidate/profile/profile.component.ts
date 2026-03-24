import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatInput, MatSuffix } from '@angular/material/input';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { UserService } from '../../../../core/services/user.service';

interface UserUi {
  fullName: string;
  address: string;
  dateOfBirth: string;
  mobile: string;
}

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatNativeDateModule,
    MatSuffix,
    MatDatepickerToggle,
    MatDatepicker,
    MatDatepickerInput,
    MatFormField,
    MatInput
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
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

  constructor(
    private readonly userService: UserService,
    private readonly toastr: NotifyMessageService
  ) {}

  ngOnInit(): void {
    this.getDetails();
  }

  getDetails(): void {
    this.userService.getDetails().pipe(take(1)).subscribe({
      next: (res) => {
        this.user = res.data;
        this.formGroup.patchValue({
          fullName: this.user.fullName,
          address: this.user.address,
          mobile: this.user.mobile,
          dateOfBirth: this.user.dateOfBirth ? new Date(this.user.dateOfBirth) : null
        });
      },
      error: () => {
        this.toastr.showMessage('Có lỗi xảy ra!', '', 'error');
      }
    });
  }

  onSubmit(): void {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }

    this.userService.updateInfo(this.formGroup.value).subscribe({
      next: (res) => {
        this.toastr.showMessage(res.message, '', 'success');
      },
      error: (err) => {
        this.toastr.showMessage(err?.error?.message || 'Có lỗi xảy ra', '', 'error');
      }
    });
  }
}
