import { Component, NO_ERRORS_SCHEMA, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { take } from 'rxjs';
import { HomeService } from '../../../home/services/home.service';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { JobService } from '../../services/job.service';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';
import { AuthService } from '../../../../core/services/auth.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-job-single',
  imports: [RouterModule, SafeHtmlPipe, TranslatePipe],
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
  hasApplied = false;

  constructor(
    private jobSerivce: JobService,
    private route: ActivatedRoute,
    private homeService: HomeService,
    private authService: AuthService,
    private i18nService: I18nService,
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
      this.checkApplyStatus();
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
        error: (err) => {
          console.error('Error fetching job details:', err);
        },
      });
  }

  checkApplyStatus(): void {
    const parsedJobId = Number(this.jobId);

    if (!this.authService.isLoggedIn() || Number.isNaN(parsedJobId) || parsedJobId <= 0) {
      this.hasApplied = false;
      return;
    }

    this.jobSerivce
      .checkApplyJob(parsedJobId)
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.hasApplied = response.data;
        },
        error: () => {
          this.hasApplied = false;
        },
      });
  }

  formatMoney(value: number): string {
    const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
    return `${value.toLocaleString(locale)} VND`;
  }
}
