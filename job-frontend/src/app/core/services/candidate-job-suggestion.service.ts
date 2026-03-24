import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { JobCardModel } from '../../shared/models/jobs/job-card.model';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';

@Injectable({
  providedIn: 'root'
})
export class CandidateJobSuggestionService {
  private readonly mockResumes: ResumeReviewInput[] = [
    { id: 2001, fileName: 'Nguyen_Frontend_Resume_2026.pdf', createDate: '2026-03-18T09:15:00' },
    { id: 2002, fileName: 'Nguyen_Backend_Resume_2026.pdf', createDate: '2026-03-05T14:40:00' },
    { id: 2003, fileName: 'Nguyen_Product_Resume_2026.pdf', createDate: '2026-02-21T08:20:00' }
  ];

  private readonly jobsByResumeId: Record<number, JobCardModel[]> = {
    2001: [
      {
        id: 401,
        title: 'Frontend Developer (Angular)',
        description: 'Xây dựng giao diện Angular, tối ưu trải nghiệm người dùng và hiệu năng hiển thị.',
        address: 'Hồ Chí Minh',
        salary: 24000000,
        time: 'Toàn thời gian'
      },
      {
        id: 402,
        title: 'UI Engineer',
        description: 'Làm việc với design system, responsive layout và các thành phần giao diện tái sử dụng.',
        address: 'Hà Nội',
        salary: 22000000,
        time: 'Làm việc kết hợp'
      },
      {
        id: 403,
        title: 'Frontend Web Developer',
        description: 'Phát triển trang quản trị và dashboard bằng Angular, TailwindCSS và REST API.',
        address: 'Đà Nẵng',
        salary: 26000000,
        time: 'Làm việc từ xa'
      }
    ],
    2002: [
      {
        id: 404,
        title: 'Backend Developer (Java Spring)',
        description: 'Phát triển API, xử lý business logic và tối ưu truy vấn cho hệ thống tuyển dụng.',
        address: 'Hồ Chí Minh',
        salary: 30000000,
        time: 'Toàn thời gian'
      },
      {
        id: 405,
        title: 'Node.js Engineer',
        description: 'Xây dựng microservice, kết nối database và triển khai quy trình theo dõi log.',
        address: 'Cần Thơ',
        salary: 28000000,
        time: 'Làm việc kết hợp'
      }
    ],
    2003: []
  };

  getCandidateResumes(): Observable<ResumeReviewInput[]> {
    return of(this.mockResumes.map((resume) => ({ ...resume }))).pipe(delay(450));
  }

  getSuggestedJobsByResume(resumeId: number): Observable<JobCardModel[]> {
    const jobs = this.jobsByResumeId[resumeId] ?? [];
    return of(jobs.map((job) => ({ ...job }))).pipe(delay(900));
  }
}
