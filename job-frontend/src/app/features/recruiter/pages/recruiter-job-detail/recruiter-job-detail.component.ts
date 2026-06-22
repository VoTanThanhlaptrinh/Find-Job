import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { JobService } from '../../../jobs/services/job.service';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { I18nService } from '../../../../core/i18n/i18n.service';

import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-recruiter-job-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './recruiter-job-detail.component.html',
  styleUrl: './recruiter-job-detail.component.css'
})
export class RecruiterJobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly jobService = inject(JobService);
  private readonly i18nService = inject(I18nService);

  isLoading = false;
  jobId = '';
  loadError: string | null = null;
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

  constructor() {
    this.jobService.resetJobDetailState();

    effect(() => {
      const jobDetail = this.jobService.jobDetail$();
      const hasError = this.jobService.jobDetailError$();

      if (jobDetail) {
        this.jobDetail = jobDetail;
        this.isLoading = false;
      }

      if (hasError) {
        this.loadError = 'Cannot load job detail right now.';
        this.isLoading = false;
      }
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id') ?? '';
      this.jobId = id;
      if (!id) {
        this.loadError = 'Job id is missing.';
        return;
      }

      this.loadJobDetail(id);
    });
  }

  formatMoney(value: number | string): string {
    if (typeof value === 'number') {
      const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
      return `${value.toLocaleString(locale)} VND`;
    }

    const normalized = value?.trim() ?? '';
    return normalized.length > 0 ? normalized : '0 VND';
  }

  private loadJobDetail(id: string): void {
    this.isLoading = true;
    this.loadError = null;

    this.jobService.getDetailJob(id);
  }
}
