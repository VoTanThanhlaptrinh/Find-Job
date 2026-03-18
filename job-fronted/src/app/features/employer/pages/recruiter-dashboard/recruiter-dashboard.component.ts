import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

type MetricCard = {
  label: string;
  value: number;
  color: string;
};

type PostedJob = {
  title: string;
  applicants: number;
  status: string;
  statusColor: string;
  tag: string;
};

type ActivityItem = {
  name: string;
  role: string;
  city: string;
  note: string;
  timeAgo: string;
};

@Component({
  selector: 'app-recruiter-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrl: './recruiter-dashboard.component.css',
})
export class RecruiterDashboardComponent {
  readonly metricCards: MetricCard[] = [
    { label: 'Việc làm Hoạt động', value: 116, color: 'bg-blue-500' },
    { label: 'Ứng viên Mới', value: 16, color: 'bg-emerald-500' },
    { label: 'Phỏng vấn', value: 144, color: 'bg-violet-500' },
  ];

  readonly chartColumns = [82, 61, 126, 92, 158, 60, 108];

  readonly postedJobs: PostedJob[] = [
    {
      title: 'UX/UI Designer - Hà Nội',
      applicants: 25,
      status: 'Đang hoạt động',
      statusColor: 'text-emerald-700 bg-emerald-100',
      tag: 'active',
    },
    {
      title: 'Kỹ sư Phần mềm - TP.HCM',
      applicants: 12,
      status: 'Đang hoạt động',
      statusColor: 'text-emerald-700 bg-emerald-100',
      tag: 'active',
    },
    {
      title: 'Marketing Lead - Đà Nẵng',
      applicants: 8,
      status: 'Sắp hết hạn',
      statusColor: 'text-amber-700 bg-amber-100',
      tag: 'expiring',
    },
  ];

  readonly activities: ActivityItem[] = [
    {
      name: 'Thinkuyen Khin Hor',
      role: 'UI/UX Designer',
      city: 'Hà Nội',
      note: '25 ứng viên, đang hoạt động',
      timeAgo: '2 phút trước',
    },
    {
      name: 'Thanhmi Then',
      role: 'Backend Developer',
      city: 'TP.HCM',
      note: '12 ứng viên, vòng CV',
      timeAgo: '10 phút trước',
    },
  ];
}
