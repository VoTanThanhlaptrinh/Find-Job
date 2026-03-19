import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { JobCardModel } from '../../models/jobs/job-card.model';

@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './job-card.component.html',
  styleUrl: './job-card.component.css',
})
export class JobCardComponent {
  @Input({ required: true }) job!: JobCardModel;
  @Input() image = 'assets/web_css/img/post.png';
  @Input() detailRoute = '/single';

  formatMoney(value: number): string {
    return `${value.toLocaleString('vi-VN')} VND`;
  }
}
