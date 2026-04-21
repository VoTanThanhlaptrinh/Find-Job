import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminJobSeekersService } from '../../services/admin-job-seekers.service';
import { AdminJobSeekerItem } from '../../services/admin-api.models';

@Component({
  selector: 'app-job-seekers',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './job-seekers.component.html'
})
export class JobSeekersComponent implements OnInit {
  private readonly jobSeekersService = inject(AdminJobSeekersService);

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

  get totalItems(): number {
    return this.jobSeekersService.totalItems();
  }

  get currentPage(): number {
    return this.jobSeekersService.currentQuery().page;
  }

  get pageSize(): number {
    return this.jobSeekersService.currentQuery().pageSize;
  }

  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
  }

  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.jobSeekersService.updateQuery({ page: 1, search: raw.length > 0 ? raw : undefined });
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

  trackBySeeker(_: number, item: AdminJobSeekerItem): string {
    return item.id;
  }
}
