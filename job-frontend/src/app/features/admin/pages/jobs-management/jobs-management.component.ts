import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminJobsService } from '../../services/admin-jobs.service';
import { AdminJobItem } from '../../services/admin-api.models';
import { take } from 'rxjs';

@Component({
  selector: 'app-jobs-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './jobs-management.component.html'
})
export class JobsManagementComponent implements OnInit {
  private readonly jobsService = inject(AdminJobsService);

  ngOnInit(): void {
    this.jobsService.loadMetrics();
    this.jobsService.loadJobs();
  }

  get metrics() {
    return this.jobsService.metrics();
  }

  get jobs() {
    return this.jobsService.jobs();
  }

  get isLoadingMetrics(): boolean {
    return this.jobsService.isLoadingMetrics();
  }

  get isLoadingList(): boolean {
    return this.jobsService.isLoadingList();
  }

  get totalItems(): number {
    return this.jobsService.totalItems();
  }

  get currentPage(): number {
    return this.jobsService.currentQuery().page;
  }

  get pageSize(): number {
    return this.jobsService.currentQuery().pageSize;
  }

  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
  }

  get updatingStatusJobId(): string | null {
    return this.jobsService.updatingStatusJobId();
  }

  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.jobsService.updateQuery({ page: 1, search: raw.length > 0 ? raw : undefined });
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

  trackByJob(_: number, item: AdminJobItem): string {
    return item.id;
  }
}
