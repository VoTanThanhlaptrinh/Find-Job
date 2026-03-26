import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

type PostedJobRow = {
  title: string;
  location: string;
  status: 'Đang mở' | 'Tạm dừng';
  applied: number;
};

@Component({
  selector: 'app-recruiter-job-list',
  imports: [CommonModule, RouterLink],
  templateUrl: './recruiter-job-list.component.html',
  styleUrl: './recruiter-job-list.component.css',
})
export class RecruiterJobListComponent {
  readonly jobs: PostedJobRow[] = [
    { title: 'Frontend Developer', location: 'TP.HCM', status: 'Đang mở', applied: 42 },
    { title: 'Product Designer', location: 'Hà Nội', status: 'Đang mở', applied: 18 },
    { title: 'QA Engineer', location: 'Đà Nẵng', status: 'Tạm dừng', applied: 9 },
  ];
}
