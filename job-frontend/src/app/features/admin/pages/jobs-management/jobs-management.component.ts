import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminJobsService } from '../../services/admin-jobs.service';
import { AdminJobItem } from '../../services/admin-api.models';
import { take } from 'rxjs';

@Component({
  selector: 'app-jobs-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './jobs-management.component.html'
})
export class JobsManagementComponent implements OnInit {
  private readonly jobsService = inject(AdminJobsService);
  private readonly fb = inject(FormBuilder);

  readonly createJobForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    companyId: ['', [Validators.required]],
    category: ['', [Validators.required]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    location: ['', [Validators.required]],
    expiryDate: ['', [Validators.required]],
  });

  readonly bulkActionForm = this.fb.nonNullable.group({
    action: ['activate', [Validators.required]],
  });

  private readonly selectedJobIds = new Set<string>();

  metrics: any = null;
  jobs: AdminJobItem[] = [];
  isLoadingMetrics = false;
  isLoadingList = false;
  totalItems = 0;
  currentPage = 1;
  pageSize = 10;
  selectedCategory = '';
  selectedStatus = '';
  totalPagesCount = 1;
  updatingStatusJobId: string | null = null;
  isCreating = false;

  constructor() {
    effect(() => {
      this.metrics = this.jobsService.metrics();
      this.jobs = this.jobsService.jobs();
      this.isLoadingMetrics = this.jobsService.isLoadingMetrics();
      this.isLoadingList = this.jobsService.isLoadingList();
      this.totalItems = this.jobsService.totalItems();
      
      const query = this.jobsService.currentQuery();
      this.currentPage = query.page ?? 1;
      this.pageSize = query.pageSize ?? 10;
      this.selectedCategory = query.category ?? '';
      this.selectedStatus = query.status ?? '';

      this.totalPagesCount = Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
      
      this.updatingStatusJobId = this.jobsService.updatingStatusJobId();
      this.isCreating = this.jobsService.isCreating();
    });
  }

  ngOnInit(): void {
    this.jobsService.loadMetrics();
    this.jobsService.loadJobs();
  }

  get totalPages(): number {
    return this.totalPagesCount;
  }

  get selectedJobsCount(): number {
    return this.selectedJobIds.size;
  }

  get areAllJobsSelected(): boolean {
    return this.jobs.length > 0 && this.selectedJobIds.size === this.jobs.length;
  }

  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.jobsService.updateQuery({ page: 1, search: raw.length > 0 ? raw : undefined });
  }

  onCategoryChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.jobsService.updateQuery({
      page: 1,
      category: value.length > 0 ? value : undefined,
    });
  }

  onStatusFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.jobsService.updateQuery({
      page: 1,
      status: value.length > 0 ? value : undefined,
    });
  }

  goPrevPage(): void {
    if (this.currentPage <= 1) {
      return;
    }

    this.jobsService.updateQuery({ page: this.currentPage - 1 });
  }

  goNextPage(): void {
    if (this.currentPage >= this.totalPages) {
      return;
    }

    this.jobsService.updateQuery({ page: this.currentPage + 1 });
  }

  updateStatus(job: AdminJobItem, status: string): void {
    if (!status || status === job.status) {
      return;
    }

    this.jobsService.updateJobStatus(job.id, { status }).pipe(take(1)).subscribe();
  }

  isCreateFieldInvalid(
    controlName: 'title' | 'companyId' | 'category' | 'description' | 'location' | 'expiryDate'
  ): boolean {
    const control = this.createJobForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  submitCreateJob(): void {
    if (this.createJobForm.invalid) {
      this.createJobForm.markAllAsTouched();
      return;
    }

    const payload = this.createJobForm.getRawValue();
    this.jobsService.createJob(payload).pipe(take(1)).subscribe({
      next: () => {
        this.createJobForm.reset({
          title: '',
          companyId: '',
          category: '',
          description: '',
          location: '',
          expiryDate: '',
        });
      }
    });
  }

  toggleSelectAll(checked: boolean): void {
    this.selectedJobIds.clear();
    if (checked) {
      for (const job of this.jobs) {
        this.selectedJobIds.add(job.id);
      }
    }
  }

  toggleJobSelection(jobId: string, checked: boolean): void {
    if (checked) {
      this.selectedJobIds.add(jobId);
      return;
    }

    this.selectedJobIds.delete(jobId);
  }

  isJobSelected(jobId: string): boolean {
    return this.selectedJobIds.has(jobId);
  }

  submitBulkAction(): void {
    if (this.bulkActionForm.invalid || this.selectedJobIds.size === 0) {
      this.bulkActionForm.markAllAsTouched();
      return;
    }

    this.jobsService.bulkAction({
      action: this.bulkActionForm.controls.action.value,
      jobIds: Array.from(this.selectedJobIds),
    }).pipe(take(1)).subscribe({
      next: () => this.selectedJobIds.clear(),
    });
  }

  trackByJob(_: number, item: AdminJobItem): string {
    return item.id;
  }
}
