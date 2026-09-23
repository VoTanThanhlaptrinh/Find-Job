import { Component, effect, OnInit, computed, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterAddressService } from '../../services/recruiter-address.service';
import { CompanyAddress } from '../company-address/company-address.component';
import { CategoryService } from '../../../../core/services/category.service';
import { Category } from '../../../../shared/models/category.model';
import { MarkdownEditorComponent } from '../../../../shared/components/markdown-editor/markdown-editor.component';
import { JobPostPreviewComponent, JobPreviewData } from '../../components/job-post-preview/job-post-preview.component';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-post-job',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    CommonModule,
    TranslatePipe,
    MarkdownEditorComponent,
    JobPostPreviewComponent
  ],
  templateUrl: './recruiter-post-job.component.html',
  styleUrl: './recruiter-post-job.component.css'
})

export class PostJobComponent implements OnInit {
  messageType: boolean | undefined = undefined;
  message = '';
  postJobFG = new FormGroup({
    jobName: new FormControl('', [Validators.required, Validators.maxLength(255)]),
    location: new FormControl('', [Validators.required]),
    jobType: new FormControl('FULL_TIME', [Validators.required]),
    salary: new FormControl('', [Validators.required, Validators.maxLength(255)]),
    headCount: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    jobDescription: new FormControl('', [Validators.required, Validators.maxLength(5000)]),
    jobRequirement: new FormControl('', [Validators.required, Validators.maxLength(5000)]),
    jobSkill: new FormControl('', [Validators.required, Validators.maxLength(5000)]),
    moreDetail: new FormControl('', [Validators.maxLength(5000)]),
    deadlineCV: new FormControl<Date | null>(null, [Validators.required, minDatePlusOne]),
    enableAiAnalysis: new FormControl(false),
    categoryId: new FormControl<number | null>(null, [Validators.required])
  });

  companyAddresses: CompanyAddress[] = [];
  isSubmitting = false;

  categorySearchTerm = signal('');
  isCategoryDropdownOpen = signal(false);
  selectedCategoryName = signal('');
  isPreviewMode = signal(false);

  filteredCategories = computed(() => {
    const term = this.categorySearchTerm().toLowerCase();
    const cats = this.categoryService.categories() || [];
    return cats.filter(c => c.name.toLowerCase().includes(term));
  });

  selectedAddressDisplay = computed(() => {
    const locId = this.postJobFG.get('location')?.value;
    if (!locId) return '';
    const addr = this.companyAddresses.find(a => a.id?.toString() === locId.toString());
    if (!addr) return '';
    return `${addr.city} - ${addr.street}`;
  });

  selectedCategoryDisplay = computed(() => {
    const catId = this.postJobFG.get('categoryId')?.value;
    if (!catId) return this.selectedCategoryName() || '';
    const cat = this.categoryService.categories()?.find(c => c.id === catId);
    return cat?.name || this.selectedCategoryName() || '';
  });

  previewData = computed<JobPreviewData>(() => ({
    jobName: this.postJobFG.get('jobName')?.value || '',
    categoryName: this.selectedCategoryDisplay(),
    jobType: this.postJobFG.get('jobType')?.value || '',
    jobTypeLabel: this.getJobTypeLabel(this.postJobFG.get('jobType')?.value),
    salary: this.postJobFG.get('salary')?.value || '',
    address: this.selectedAddressDisplay(),
    headcount: this.postJobFG.get('headCount')?.value ?? null,
    deadlineCV: this.formatDateDisplay(this.postJobFG.get('deadlineCV')?.value),
    enableAiAnalysis: !!this.postJobFG.get('enableAiAnalysis')?.value,
    jobDescription: this.postJobFG.get('jobDescription')?.value || '',
    jobRequirement: this.postJobFG.get('jobRequirement')?.value || '',
    jobSkill: this.postJobFG.get('jobSkill')?.value || '',
    moreDetail: this.postJobFG.get('moreDetail')?.value || ''
  }));    

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService,
    private readonly addressService: RecruiterAddressService,
    private readonly categoryService: CategoryService,
    private readonly i18nService: I18nService
  ) {
    effect(() => {
      this.isSubmitting = this.recruiterJobsService.isSubmittingJob$();
      const actionTick = this.recruiterJobsService.actionTick$();
      const actionType = this.recruiterJobsService.lastActionType$();
      if (actionTick === 0 || actionType !== 'create') {
        return;
      }

      const isSuccess = this.recruiterJobsService.lastActionSuccess$();
      const message = this.recruiterJobsService.lastActionMessage$();
      this.messageType = isSuccess;
      this.message = message;

      if (isSuccess) {
        this.notify.showMessage(message, '', 'success');
        this.postJobFG.reset({
          jobType: 'FULL_TIME',
          deadlineCV: null,
          enableAiAnalysis: false
        });
        this.isPreviewMode.set(false);
        return;
      }

      this.notify.showMessage(message, '', 'error');
    });
  }

  ngOnInit(): void {
    this.categoryService.loadCategories();
    this.addressService.getAddresses().subscribe({
      next: (response) => {
        if (response.status === 200) {
          this.companyAddresses = response.data;
          
          // Set default address if available
          const defaultAddress = this.companyAddresses.find(a => a.isDefault);
          if (defaultAddress) {
            this.postJobFG.get('location')?.setValue(defaultAddress.id.toString());
          }
        }
      },
      error: (err) => {
        console.error('Failed to load company addresses', err);
      }
    });
  }

  toggleCategoryDropdown() {
    this.isCategoryDropdownOpen.update(v => !v);
  }

  closeCategoryDropdown() {
    this.postJobFG.get('categoryId')?.markAsTouched();
    setTimeout(() => {
      this.isCategoryDropdownOpen.set(false);
    }, 200);
  }

  selectCategory(cat: Category) {
    this.postJobFG.get('categoryId')?.setValue(cat.id);
    this.selectedCategoryName.set(cat.name);
    this.categorySearchTerm.set('');
    this.isCategoryDropdownOpen.set(false);
  }

  onCategorySearch(event: Event) {
    const val = (event.target as HTMLInputElement).value;
    this.categorySearchTerm.set(val);
    
    if (this.selectedCategoryName()) {
      this.selectedCategoryName.set('');
      this.postJobFG.get('categoryId')?.setValue(null);
    } else if (!val) {
      this.postJobFG.get('categoryId')?.setValue(null);
      this.selectedCategoryName.set('');
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.postJobFG.get(fieldName);
    return !!(control && control.invalid && (control.touched || control.dirty));
  }

  getFieldError(fieldName: string): string | null {
    const control = this.postJobFG.get(fieldName);
    if (!control || !control.errors || !(control.touched || control.dirty)) return null;

    if (control.errors['required']) {
      switch (fieldName) {
        case 'jobName': return this.i18nService.translate('recruiterPostJob.validation.jobNameRequired');
        case 'categoryId': return this.i18nService.translate('recruiterPostJob.validation.categoryRequired');
        case 'jobType': return this.i18nService.translate('recruiterPostJob.validation.jobTypeRequired');
        case 'location': return this.i18nService.translate('recruiterPostJob.validation.locationRequired');
        case 'salary': return this.i18nService.translate('recruiterPostJob.validation.salaryRequired');
        case 'headCount': return this.i18nService.translate('recruiterPostJob.validation.headcountRequired');
        case 'deadlineCV': return this.i18nService.translate('recruiterPostJob.validation.deadlineRequired');
        case 'jobDescription': return this.i18nService.translate('recruiterPostJob.validation.descriptionRequired');
        case 'jobSkill': return this.i18nService.translate('recruiterPostJob.validation.skillsRequired');
        case 'jobRequirement': return this.i18nService.translate('recruiterPostJob.validation.requirementsRequired');
        default: return this.i18nService.translate('recruiterPostJob.validation.required');
      }
    }

    if (control.errors['min']) {
      if (fieldName === 'headCount') return this.i18nService.translate('recruiterPostJob.validation.headcountMin');
      return `Min ${control.errors['min'].min}`;
    }

    if (control.errors['maxlength']) {
      switch (fieldName) {
        case 'jobName': return this.i18nService.translate('recruiterPostJob.validation.jobNameMax');
        case 'salary': return this.i18nService.translate('recruiterPostJob.validation.salaryMax');
        case 'jobDescription': return this.i18nService.translate('recruiterPostJob.validation.descriptionMax');
        case 'jobSkill': return this.i18nService.translate('recruiterPostJob.validation.skillsMax');
        case 'jobRequirement': return this.i18nService.translate('recruiterPostJob.validation.requirementsMax');
        case 'moreDetail': return this.i18nService.translate('recruiterPostJob.validation.benefitsMax');
        default: return `Max ${control.errors['maxlength'].requiredLength}`;
      }
    }

    if (control.errors['minDatePlusOne']) {
      return this.i18nService.translate('recruiterPostJob.validation.deadlineFuture');
    }

    return null;
  }

  onSubmit() {
    if (this.postJobFG.invalid) {
      this.postJobFG.markAllAsTouched();
      this.notify.showMessage(this.i18nService.translate('recruiterPostJob.validation.formInvalid'), '', 'error');
      return;
    }
    const formValue = this.postJobFG.value;
    const formData = new FormData();
    Object.entries(formValue).forEach(([key, val]) => {
      if (val !== null && val !== undefined) {
          let formKey = key;
          if (key === 'location') formKey = 'addressId';
          if (key === 'headCount') formKey = 'headcount';
          if (val instanceof File) {
            formData.append(formKey, val, val.name);
          } else if (val instanceof Date) {
            formData.append(formKey, val.toISOString().split('T')[0]);
          } else if (typeof val === 'boolean') {
            formData.append(formKey, String(val));
          } else {
            formData.append(formKey, String(val));
          }
      }
    });
    this.recruiterJobsService.createJob(formData);
  }
  
  get f() {
    return this.postJobFG.controls;
  }

  openPreview(): void {
    if (this.postJobFG.invalid) {
      this.postJobFG.markAllAsTouched();
      this.notify.showMessage(this.i18nService.translate('recruiterPostJob.validation.formInvalid'), '', 'error');
      return;
    }
    this.isPreviewMode.set(true);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  closePreview(): void {
    this.isPreviewMode.set(false);
    if (typeof window !== 'undefined') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  getJobTypeLabel(type?: string | null): string {
    switch (type) {
      case 'FULL_TIME': return this.i18nService.translate('recruiterPostJob.jobTypeOptions.fullTime');
      case 'PART_TIME': return this.i18nService.translate('recruiterPostJob.jobTypeOptions.partTime');
      case 'HYBRID': return this.i18nService.translate('recruiterPostJob.jobTypeOptions.hybrid');
      case 'REMOTE': return this.i18nService.translate('recruiterPostJob.jobTypeOptions.remote');
      default: return type || '';
    }
  }

  formatDateDisplay(dateVal: any): string {
    if (!dateVal) return '';
    const d = new Date(dateVal);
    if (isNaN(d.getTime())) return String(dateVal);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}/${month}/${year}`;
  }


}
export function minDatePlusOne(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (!value) return null;

  const selected = new Date(value);
  const today = new Date();
  const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);

  if (isNaN(selected.getTime()) || selected <= tomorrow) {
    return { minDatePlusOne: { requiredDate: tomorrow.toISOString().split('T')[0] } };
  }
  return null;
}


