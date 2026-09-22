import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepicker, MatDatepickerInput, MatDatepickerToggle } from '@angular/material/datepicker';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { effect } from '@angular/core';
import { UserService } from '../../../core/services/user.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { SavedJobsService } from '../../../core/services/saved-jobs.service';
import { ProfileSavedJobsComponent } from './components/profile-saved-jobs/profile-saved-jobs.component';
import { ProfileMyBlogComponent } from './components/profile-my-blog/profile-my-blog.component';

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
    TranslatePipe,
    ProfileSavedJobsComponent,
    ProfileMyBlogComponent
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  activeTab: 'info' | 'saved-jobs' | 'my-blog' = 'info';

  private readonly userService = inject(UserService);
  private readonly toastr = inject(NotifyMessageService);
  private readonly i18n = inject(I18nService);
  private readonly savedJobsService = inject(SavedJobsService);

  get savedJobCount(): number {
    return this.savedJobsService.savedJobIds().size;
  }
  blogTotalElements = 0;

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

  setActiveTab(tab: 'info' | 'saved-jobs' | 'my-blog'): void {
    this.activeTab = tab;
  }

  onTotalBlogsChange(count: number): void {
    this.blogTotalElements = count;
  }

  onSubmit(): void {
    if (this.formGroup.invalid) {
      this.formGroup.markAllAsTouched();
      return;
    }

    this.userService.updateInfo(this.formGroup.value).subscribe({
      next: () => {
        this.toastr.showMessage(this.i18n.translate('profile.success'), '', 'success');
      },
      error: (err) => {
        this.toastr.showMessage(err?.error?.message || this.i18n.translate('profile.error'), '', 'error');
      }
    });
  }
}
