import { Component, NO_ERRORS_SCHEMA, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { take } from 'rxjs';
import { HomeService } from '../../../home/services/home.service';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { JobServiceService } from '../../services/job.service';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';

@Component({
  selector: 'app-job-single',
  imports: [RouterModule, SafeHtmlPipe],
  standalone: true,
  templateUrl: './job-single.component.html',
  styleUrl: './job-single.component.css',
  schemas: [NO_ERRORS_SCHEMA],
})
export class JobSingleComponent implements OnInit {
  jobId!: string;
  jobDetail: JobDetailViewModel = {
    id: '',
    title: '',
    address: '',
    description: '',
    salary: 0,
    time: '',
    requireDetails: '',
    skill: '',
    expiredDate: '',
  };
  relatedJobs: JobCardModel[] = [];

  constructor(
    private jobSerivce: JobServiceService,
    private route: ActivatedRoute,
    private homeService: HomeService
  ) {}

  carouselOptions = {
    loop: false,
    rewind: true,
    mouseDrag: true,
    touchDrag: true,
    pullDrag: false,
    dots: false,
    navSpeed: 700,
    margin: 10,
    autoplay: true,
    autoplayTimeout: 2000,
    responsive: {
      0: { items: 1 },
      600: { items: 2 },
      1000: { items: 3 },
    },
    nav: true,
  };

  ngOnInit(): void {
    
    this.route.params.subscribe((params) => {
      this.jobId = params['id'];
      this.getDetailJob(this.jobId);
    });
  }

  getDetailJob(id: string): void {
    this.jobSerivce
      .getDetailJob(id)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.jobDetail = response.data;
        },
        error: (error) => {
          console.error('Error fetching job details:', error);
        },
      });
  }

  formatMoney(value: number): string {
    return `${value.toLocaleString('vi-VN')} VND`;
  }
}
