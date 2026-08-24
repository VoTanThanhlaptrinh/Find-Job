import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-job-pagination',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './job-pagination.component.html',
  styleUrl: './job-pagination.component.css',
})
export class JobPaginationComponent {
  @Input() totalItems = 0;
  @Input() pageSize = 10;
  @Input() currentPage = 1; // 1-based page number

  @Output() pageChange = new EventEmitter<number>();

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.pageSize));
  }

  get startIndex(): number {
    if (this.totalItems === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get endIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  get visiblePages(): Array<number | 'ellipsis'> {
    const total = this.totalPages;
    if (total <= 6) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    const current = this.currentPage;
    const pages = new Set<number>([1, total, current, current - 1, current + 1]);
    const sorted = Array.from(pages)
      .filter((p) => p >= 1 && p <= total)
      .sort((a, b) => a - b);

    const items: Array<number | 'ellipsis'> = [];
    sorted.forEach((page, idx) => {
      if (idx > 0 && page - sorted[idx - 1] > 1) {
        items.push('ellipsis');
      }
      items.push(page);
    });

    return items;
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages || page === this.currentPage) return;
    this.pageChange.emit(page);
  }

  goToPrev(): void {
    this.goToPage(this.currentPage - 1);
  }

  goToNext(): void {
    this.goToPage(this.currentPage + 1);
  }
}
