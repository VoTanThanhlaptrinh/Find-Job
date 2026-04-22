import { Component, OnInit, inject } from '@angular/core';
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

  ngOnInit(): void {
    this.jobSeekersService.loadMetrics();
    this.jobSeekersService.loadJobSeekers();
    this.jobSeekersService.loadRegionDistribution();
  }

  get metrics() {
    return this.jobSeekersService.metrics();
  }

  get jobSeekers() {
    return this.jobSeekersService.jobSeekers();
  }

  get regionDistribution() {
    return this.jobSeekersService.regionDistribution();
  }

  get isLoadingMetrics(): boolean {
    return this.jobSeekersService.isLoadingMetrics();
  }

  get isLoadingList(): boolean {
    return this.jobSeekersService.isLoadingList();
  }

  get isLoadingRegions(): boolean {
    return this.jobSeekersService.isLoadingRegions();
  }

  get isCreating(): boolean {
    return this.jobSeekersService.isCreating();
  }

  get totalItems(): number {
    return this.jobSeekersService.totalItems();
  }

  get currentPage(): number {
    return this.jobSeekersService.currentQuery().page;
  }

  get pageSize(): number {
    return this.jobSeekersService.currentQuery().pageSize;
  }

  get selectedResumeStatus(): string {
    return this.jobSeekersService.currentQuery().resumeStatus ?? '';
  }

  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
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
