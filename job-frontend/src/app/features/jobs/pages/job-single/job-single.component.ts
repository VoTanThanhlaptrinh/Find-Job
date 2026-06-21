import { Component, effect, NO_ERRORS_SCHEMA, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { JobService } from '../../services/job.service';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';
import { AuthService } from '../../../../core/services/auth.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { SkeletonJobSingleComponent } from '../../../../shared/components/skeleton-job-single/skeleton-job-single.component';

@Component({
  selector: 'app-job-single',
  imports: [RouterModule, SafeHtmlPipe, TranslatePipe, SkeletonJobSingleComponent],
  standalone: true,
  templateUrl: './job-single.component.html',
  styleUrl: './job-single.component.css',
  schemas: [NO_ERRORS_SCHEMA],
})
export class JobSingleComponent implements OnInit {
  jobId!: string;
  private readonly currentJobId = signal<string>('');
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
    headcount: 0
  };
  relatedJobs: JobCardModel[] = [];
  hasApplied: boolean | null = null;
  isCheckingApply = false;
  isLoading = false;

  constructor(
    private jobSerivce: JobService,
    private route: ActivatedRoute,
    private authService: AuthService,
    private i18nService: I18nService,
  ) {
    this.jobSerivce.resetJobDetailState();
    this.jobSerivce.resetCheckApplyState();

    effect(() => {
      this.jobDetail = this.jobSerivce.jobDetail$() ?? {
        id: '',
        title: '',
        address: '',
        description: '',
        salary: 0,
        time: '',
        requireDetails: '',
        skill: '',
        expiredDate: '',
        headcount: 0
      };
      this.hasApplied = this.jobSerivce.hasApplied$();
      this.isCheckingApply = this.jobSerivce.isCheckingApply$();
      this.isLoading = this.jobSerivce.isLoadingJobDetail$();
    });

    effect(() => {
      const jobId = this.currentJobId();
      const isAuthReady = this.authService.isAuthReady();
      const isLoggedIn = this.authService.isLoggedIn();

      if (!jobId || !isAuthReady) {
        return;
      }

      const parsedJobId = Number(jobId);
      if (!isLoggedIn || Number.isNaN(parsedJobId) || parsedJobId <= 0) {
        this.jobSerivce.resetCheckApplyState();
        return;
      }

      this.jobSerivce.checkApplyJob(parsedJobId);
    });
  }

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
      this.currentJobId.set(this.jobId);
      this.getDetailJob(this.jobId);
    });
  }

  getDetailJob(id: string): void {
    this.jobSerivce.getDetailJob(id);
  }

  checkApplyStatus(): void {
    const parsedJobId = Number(this.jobId);

    if (!this.authService.isLoggedIn() || Number.isNaN(parsedJobId) || parsedJobId <= 0) {
      this.jobSerivce.resetCheckApplyState();
      this.hasApplied = null;
      return;
    }

    this.jobSerivce.checkApplyJob(parsedJobId);
  }

  get showApplyButton(): boolean {
    if (!this.authService.isAuthReady()) {
      return false;
    }

    if (!this.authService.isLoggedIn()) {
      return true;
    }

    return !this.isCheckingApply && this.hasApplied === false;
  }

  get showApplyDisabledState(): boolean {
    if (!this.authService.isAuthReady()) {
      return true;
    }

    return this.authService.isLoggedIn() && this.isCheckingApply;
  }

  formatMoney(value: number): string {
    const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
    return `${value.toLocaleString(locale)} VND`;
  }
}
