import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HirerJobPostView } from '../../../../shared/models/jobs/job-api-response.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-hirer-job-card',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './hirer-job-card.component.html',
  styleUrl: './hirer-job-card.component.css'
})
export class HirerJobCardComponent {
  @Input({ required: true }) job!: HirerJobPostView;
  @Input() detailRoute = '/recruiter/jobs/detail';
  @Input() image = 'assets/web_css/img/post.png';

  @Output() delete = new EventEmitter<number>();
  @Output() analyze = new EventEmitter<number>();

  get appliesRate(): number {
    if (!this.job?.headcount || this.job.headcount <= 0) {
      return 0;
    }

    return Math.min(100, Math.round((this.job.applies / this.job.headcount) * 100));
  }

  get shortDescription(): string {
    const content = this.job?.description?.trim() ?? '';
    if (content.length <= 160) {
      return content;
    }

    return `${content.slice(0, 160).trim()}...`;
  }

  formatMoney(value: any): string {
    if (!value) {
      return 'Đang cập nhật';
    }
    const num = Number(value);
    if (Number.isNaN(num)) {
      return value;
    }
    return `${num.toLocaleString('vi-VN')} VND`;
  }

  statusClass(status?: string): string {
    const normalized = (status ?? '').toLowerCase();
    if (normalized.includes('active') || normalized.includes('open')) {
      return 'bg-emerald-100 text-emerald-700 border border-emerald-200';
    }
    if (normalized.includes('draft') || normalized.includes('pending')) {
      return 'bg-amber-100 text-amber-700 border border-amber-200';
    }
    return 'bg-slate-100 text-slate-700 border border-slate-200';
  }

  formatStatus(status?: string): string {
    if (!status) {
      return 'Không rõ';
    }
    const normalized = status.trim();
    if (normalized.length === 0) {
      return 'Không rõ';
    }
    return normalized;
  }

  onDelete(event: Event, id: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.delete.emit(id);
  }

  onAnalyze(event: Event, id: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.analyze.emit(id);
  }
}

