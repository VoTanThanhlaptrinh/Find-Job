import { Component, OnInit, inject, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { take } from 'rxjs';
import { AdminJobSeekersService } from '../../services/admin-job-seekers.service';
import { AdminJobSeekerItem } from '../../services/admin-api.models';

@Component({
  selector: 'app-job-seekers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-seekers.component.html'
})
export class JobSeekersComponent implements OnInit {
  private readonly jobSeekersService = inject(AdminJobSeekersService);
  private readonly fb = inject(FormBuilder);
  readonly createJobSeekerForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    profession: ['', [Validators.required]],
    resumeUrl: ['', [Validators.required]],
  });

  metrics: any = null;
  jobSeekers: AdminJobSeekerItem[] = [];
  regionDistribution: any = null;
  isLoadingMetrics = false;
  isLoadingList = false;
  isLoadingRegions = false;
  isCreating = false;
  totalItems = 0;
  currentPage = 1;
  pageSize = 10;
  selectedResumeStatus = '';
  totalPagesCount = 1;

  constructor() {
    effect(() => {
      this.metrics = this.jobSeekersService.metrics();
      this.jobSeekers = this.jobSeekersService.jobSeekers();
      this.regionDistribution = this.jobSeekersService.regionDistribution();
      this.isLoadingMetrics = this.jobSeekersService.isLoadingMetrics();
      this.isLoadingList = this.jobSeekersService.isLoadingList();
      this.isLoadingRegions = this.jobSeekersService.isLoadingRegions();
      this.isCreating = this.jobSeekersService.isCreating();
      this.totalItems = this.jobSeekersService.totalItems();
      
      const query = this.jobSeekersService.currentQuery();
      this.currentPage = query.page ?? 1;
      this.pageSize = query.pageSize ?? 10;
      this.selectedResumeStatus = query.resumeStatus ?? '';
      
      this.totalPagesCount = Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
    });
  }

  ngOnInit(): void {
    this.jobSeekersService.loadMetrics();
    this.jobSeekersService.loadJobSeekers();
    this.jobSeekersService.loadRegionDistribution();
  }

  get totalPages(): number {
    return this.totalPagesCount;
  }

  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.jobSeekersService.updateQuery({ page: 1, search: raw.length > 0 ? raw : undefined });
  }

  onResumeStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.jobSeekersService.updateQuery({
      page: 1,
      resumeStatus: value.length > 0 ? value : undefined,
    });
  }

  goPrevPage(): void {
    if (this.currentPage <= 1) {
      return;
    }

    this.jobSeekersService.updateQuery({ page: this.currentPage - 1 });
  }

  goNextPage(): void {
    if (this.currentPage >= this.totalPages) {
      return;
    }

    this.jobSeekersService.updateQuery({ page: this.currentPage + 1 });
  }

  isCreateFieldInvalid(controlName: 'fullName' | 'email' | 'profession' | 'resumeUrl'): boolean {
    const control = this.createJobSeekerForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  submitCreateJobSeeker(): void {
    if (this.createJobSeekerForm.invalid) {
      this.createJobSeekerForm.markAllAsTouched();
      return;
    }

    const payload = this.createJobSeekerForm.getRawValue();
    this.jobSeekersService.createJobSeeker(payload).pipe(take(1)).subscribe({
      next: () => {
        this.createJobSeekerForm.reset({
          fullName: '',
          email: '',
          profession: '',
          resumeUrl: '',
        });
      }
    });
  }

  trackBySeeker(_: number, item: AdminJobSeekerItem): string {
    return item.id;
  }
}
