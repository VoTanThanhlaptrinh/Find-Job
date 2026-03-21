import { Component, OnInit } from '@angular/core';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { LoadingComponent } from '../../../../shared/components/loading/loading.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';

@Component({
  selector: 'app-history-apply',
  standalone: true,
  imports: [JobCardComponent, LoadingComponent, SkeletonJobCardComponent],
  templateUrl: './history-apply.component.html',
  styleUrl: './history-apply.component.css'
})
export class HistoryApplyComponent implements OnInit {
  isLoading = true;
  skeleton = this.getSkeletonFlag();
  readonly skeletonRows = [1, 2, 3];

  readonly appliedJobs: JobCardModel[] = [
    {
      id: 101,
      title: 'Frontend Developer (Angular)',
      description: 'Phat trien giao dien web, toi uu hieu nang va trai nghiem nguoi dung.',
      address: 'Ho Chi Minh',
      salary: 25000000,
      time: 'Da ung tuyen 2 ngay truoc'
    },
    {
      id: 102,
      title: 'Backend Developer (Java Spring)',
      description: 'Xay dung API, toi uu truy van va trien khai he thong on dinh.',
      address: 'Da Nang',
      salary: 30000000,
      time: 'Da ung tuyen 5 ngay truoc'
    },
    {
      id: 103,
      title: 'Fullstack Engineer',
      description: 'Tham gia phat trien end-to-end, phoi hop voi team product va QA.',
      address: 'Ha Noi',
      salary: 35000000,
      time: 'Da ung tuyen 1 tuan truoc'
    }
  ];

  ngOnInit(): void {
    // Simulate API latency to show loading/skeleton state in this demo page.
    setTimeout(() => {
      this.isLoading = false;
    }, 1200);
  }

  private getSkeletonFlag(): boolean {
    return true;
  }

  get totalAppliedJobs(): number {
    return this.appliedJobs.length;
  }

  get averageSalaryLabel(): string {
    if (this.appliedJobs.length === 0) {
      return '--';
    }

    const totalSalary = this.appliedJobs.reduce((sum, job) => sum + job.salary, 0);
    const averageSalary = totalSalary / this.appliedJobs.length;
    return `${Math.round(averageSalary).toLocaleString('vi-VN')} VND`;
  }

  get highestSalaryLabel(): string {
    if (this.appliedJobs.length === 0) {
      return '--';
    }

    const highestSalary = Math.max(...this.appliedJobs.map(job => job.salary));
    return `${highestSalary.toLocaleString('vi-VN')} VND`;
  }

  get latestAppliedTime(): string {
    if (this.appliedJobs.length === 0) {
      return '--';
    }

    return this.appliedJobs[0].time;
  }
}
