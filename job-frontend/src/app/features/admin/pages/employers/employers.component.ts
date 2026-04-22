import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminEmployersService } from '../../services/admin-employers.service';
import { AdminEmployerItem, AdminEmployerStatusAction } from '../../services/admin-api.models';
import { finalize, take } from 'rxjs';

@Component({
  selector: 'app-employers',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employers.component.html'
})
export class EmployersComponent implements OnInit {
  private readonly employersService = inject(AdminEmployersService);
  isExporting = false;

  ngOnInit(): void {
    this.employersService.loadMetrics();
    this.employersService.loadEmployers();
  }

  get metrics() {
    return this.employersService.metrics();
  }

  get employers() {
    return this.employersService.employers();
  }

  get selectedEmployer() {
    return this.employersService.selectedEmployer();
  }

  get isLoadingMetrics(): boolean {
    return this.employersService.isLoadingMetrics();
  }

  get isLoadingList(): boolean {
    return this.employersService.isLoadingList();
  }

  get currentPage(): number {
    return this.employersService.currentQuery().page;
  }

  get totalItems(): number {
    return this.employersService.totalItems();
  }

  get pageSize(): number {
    return this.employersService.currentQuery().pageSize;
  }

  get selectedKycStatus(): string {
    return this.employersService.currentQuery().kycStatus ?? '';
  }

  get selectedAccountStatus(): string {
    return this.employersService.currentQuery().status ?? '';
  }

  get totalPages(): number {
    return Math.max(Math.ceil(this.totalItems / this.pageSize), 1);
  }

  onSearch(event: Event): void {
    const raw = (event.target as HTMLInputElement).value.trim();
    this.employersService.updateQuery({ page: 1, search: raw.length > 0 ? raw : undefined });
  }

  onKycStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.employersService.updateQuery({
      page: 1,
      kycStatus: value.length > 0 ? value : undefined,
    });
  }

  onAccountStatusChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.employersService.updateQuery({
      page: 1,
      status: value.length > 0 ? value : undefined,
    });
  }

  goPrevPage(): void {
    if (this.currentPage <= 1) {
      return;
    }

    this.employersService.updateQuery({ page: this.currentPage - 1 });
  }

  goNextPage(): void {
    if (this.currentPage >= this.totalPages) {
      return;
    }

    this.employersService.updateQuery({ page: this.currentPage + 1 });
  }

  selectEmployer(id: string): void {
    this.employersService.loadEmployerDetail(id);
  }

  changeStatus(employer: AdminEmployerItem, action: AdminEmployerStatusAction): void {
    const reason = action === 'suspend' ? window.prompt('Suspend reason (optional):')?.trim() : undefined;
    this.employersService.updateStatus(employer.id, {
      action,
      reason: reason || undefined,
    }).pipe(take(1)).subscribe();
  }

  exportEmployers(): void {
    const query = this.employersService.currentQuery();
    this.isExporting = true;
    this.employersService.getExportUrl({
      format: 'csv',
      search: query.search,
      kycStatus: query.kycStatus,
      status: query.status,
    }).pipe(
      take(1),
      finalize(() => {
        this.isExporting = false;
      })
    ).subscribe({
      next: (res) => {
        if (res.downloadUrl) {
          window.open(res.downloadUrl, '_blank', 'noopener');
        }
      }
    });
  }

  trackByEmployer(_: number, item: AdminEmployerItem): string {
    return item.id;
  }
}
