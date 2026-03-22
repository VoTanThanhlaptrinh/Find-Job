import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type DashboardMetric = {
  title: string;
  value: string;
  trend: string;
};

@Component({
  selector: 'app-recruiter-dashboard-overview',
  imports: [CommonModule],
  templateUrl: './recruiter-dashboard-overview.component.html',
  styleUrl: './recruiter-dashboard-overview.component.css',
})
export class RecruiterDashboardOverviewComponent {
  readonly metrics: DashboardMetric[] = [
    { title: 'Việc làm đang mở', value: '12', trend: '+2 tuần này' },
    { title: 'Ứng viên mới', value: '34', trend: '+8 hôm nay' },
    { title: 'Phỏng vấn đã lên lịch', value: '9', trend: '+3 trong 7 ngày' },
    { title: 'Tỉ lệ phản hồi', value: '86%', trend: '+4%' },
  ];
}
