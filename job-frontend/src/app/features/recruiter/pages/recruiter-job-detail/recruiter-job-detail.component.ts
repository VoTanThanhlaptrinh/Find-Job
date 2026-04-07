import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { take } from 'rxjs';
import { JobService } from '../../../jobs/services/job.service';
import { JobDetailViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-recruiter-job-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, SafeHtmlPipe, TranslatePipe],
  templateUrl: './recruiter-job-detail.component.html',
  styleUrl: './recruiter-job-detail.component.css'
})
export class RecruiterJobDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly jobService = inject(JobService);
  private readonly i18nService = inject(I18nService);

  readonly isLoading = signal<boolean>(false);
  readonly jobId = signal<string>('');
  readonly loadError = signal<string | null>(null);
  readonly jobDetail = signal<JobDetailViewModel>({
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
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id') ?? '';
      this.jobId.set(id);
      if (!id) {
        this.loadError.set('Job id is missing.');
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
    this.isLoading.set(true);
    this.loadError.set(null);

    this.jobService.getDetailJob(id).pipe(take(1)).subscribe({
      next: (response) => {
        this.jobDetail.set(response.data);
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Cannot load job detail right now.');
        this.isLoading.set(false);
      }
    });
  }
}
