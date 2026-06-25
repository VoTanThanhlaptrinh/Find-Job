import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { JobMatchView } from '../../models/jobs/job-match-view.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-job-match-card',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './job-match-card.component.html',
  styleUrl: './job-match-card.component.css',
})
export class JobMatchCardComponent {
  @Input({ required: true }) job!: JobMatchView;
  @Input() image = 'assets/web_css/img/post.png';
  @Input() detailRoute = '/single';

  formatMoney(value: number | string): string {
    if (typeof value === 'number') {
      return `${value.toLocaleString('vi-VN')}`;
    }
    return value;
  }
}
