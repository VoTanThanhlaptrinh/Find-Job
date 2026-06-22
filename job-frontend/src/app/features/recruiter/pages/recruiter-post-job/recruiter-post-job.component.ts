import { Component, effect, OnInit, computed, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule, ValidationErrors,
  Validators
} from '@angular/forms';
import { QuillModule } from 'ngx-quill';
import { CommonModule } from '@angular/common';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { RecruiterJobsService } from '../../services/recruiter-jobs.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { RecruiterAddressService } from '../../services/recruiter-address.service';
import { CompanyAddress } from '../company-address/company-address.component';
import { CategoryService } from '../../../../core/services/category.service';
import { Category } from '../../../../shared/models/category.model';

@Component({
  selector: 'app-post-job',
  imports: [
    FormsModule,
    QuillModule,
    ReactiveFormsModule,
    CommonModule,
    TranslatePipe
  ],
  templateUrl: './recruiter-post-job.component.html',
  styleUrl: './recruiter-post-job.component.css'
})

export class PostJobComponent implements OnInit {
  messageType: boolean | undefined = undefined;
  message = '';
  postJobFG = new FormGroup({
    jobName: new FormControl('', [Validators.required]),
    location: new FormControl('', [Validators.required]),
    jobType: new FormControl('FULL_TIME', [Validators.required]),
    salary: new FormControl('', [Validators.required, Validators.min(2000000)]),
    headCount: new FormControl('', [Validators.required, Validators.min(1)]),
    jobDescription: new FormControl('', [Validators.required]),
    jobRequirement: new FormControl('', [Validators.required]),
    jobSkill: new FormControl('', [Validators.required]),
    moreDetail: new FormControl(''),
    deadlineCV: new FormControl<Date|null>(null, [Validators.required, minDatePlusOne]),
    enableAiAnalysis: new FormControl(false),
    categoryId: new FormControl<number|null>(null, [Validators.required])
  });

  companyAddresses: CompanyAddress[] = [];
  isSubmitting = false;

  categorySearchTerm = signal('');
  isCategoryDropdownOpen = signal(false);
  selectedCategoryName = signal('');

  filteredCategories = computed(() => {
    const term = this.categorySearchTerm().toLowerCase();
    const cats = this.categoryService.categories() || [];
    return cats.filter(c => c.name.toLowerCase().includes(term));
  });    

  constructor(
    private readonly recruiterJobsService: RecruiterJobsService,
    private readonly notify: NotifyMessageService,
    private readonly addressService: RecruiterAddressService,
    private readonly categoryService: CategoryService
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
  onSubmit() {
    if(this.postJobFG.invalid){
      this.postJobFG.markAllAsTouched();
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


