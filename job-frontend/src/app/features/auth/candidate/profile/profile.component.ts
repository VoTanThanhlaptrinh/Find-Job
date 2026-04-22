import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, computed } from '@angular/core';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { MatFormField, MatInput, MatSuffix } from '@angular/material/input';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { UserService } from '../../../../core/services/user.service';
import { I18nService } from '../../../../core/i18n/i18n.service';

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
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly toastr = inject(NotifyMessageService);
  private readonly i18n = inject(I18nService);

  readonly title = computed(() => this.i18n.translate('profile.title'));
  readonly subtitle = computed(() => this.i18n.translate('profile.subtitle'));
  readonly basicInfoLabel = computed(() => this.i18n.translate('profile.basicInfo'));
  readonly professionalInfoLabel = computed(() => this.i18n.translate('profile.professionalInfo'));
  readonly fullNameLabel = computed(() => this.i18n.translate('profile.fullName'));
  readonly fullNamePlaceholder = computed(() => this.i18n.translate('profile.fullNamePlaceholder'));
  readonly fullNameHint = computed(() => this.i18n.translate('profile.fullNameHint'));
  readonly dobLabel = computed(() => this.i18n.translate('profile.dob'));
  readonly phoneLabel = computed(() => this.i18n.translate('profile.phone'));
  readonly phonePlaceholder = computed(() => this.i18n.translate('profile.phonePlaceholder'));
  readonly addressLabel = computed(() => this.i18n.translate('profile.address'));
  readonly addressPlaceholder = computed(() => this.i18n.translate('profile.addressPlaceholder'));
  readonly bioLabel = computed(() => this.i18n.translate('profile.bio'));
  readonly bioPlaceholder = computed(() => this.i18n.translate('profile.bioPlaceholder'));
  readonly backLabel = computed(() => this.i18n.translate('profile.back'));
  readonly cancelLabel = computed(() => this.i18n.translate('profile.cancel'));
  readonly updateLabel = computed(() => this.i18n.translate('profile.update'));

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
        this.toastr.showMessage(this.i18n.translate('profile.error'), '', 'error');
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
        this.toastr.showMessage(this.i18n.translate('profile.success'), '', 'success');
      },
      error: (err) => {
        this.toastr.showMessage(err?.error?.message || this.i18n.translate('profile.error'), '', 'error');
      }
    });
  }
}
