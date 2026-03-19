import { Component, Input } from '@angular/core';

export interface JobCardData {
  id: string | number;
  title: string;
  address: string;
  image: string;
  link: string;
  description: string;
  salary: number;
  type: string;
}

@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [],
  templateUrl: './job-card.component.html',
  styleUrl: './job-card.component.css',
})
export class JobCardComponent {
  @Input({ required: true }) job!: JobCardData;

  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
  }
}
