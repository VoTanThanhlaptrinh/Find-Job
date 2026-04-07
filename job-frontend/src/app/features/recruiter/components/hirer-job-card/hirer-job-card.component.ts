import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
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
  @Input() detailRoute = '/single';

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
}
