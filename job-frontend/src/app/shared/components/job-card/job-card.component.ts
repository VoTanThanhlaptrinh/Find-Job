import { CommonModule } from '@angular/common';
import { Component, computed, inject, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SavedJobsService } from '../../../core/services/saved-jobs.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { JobCardModel } from '../../models/jobs/job-card.model';

@Component({
  selector: 'app-job-card',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './job-card.component.html',
  styleUrl: './job-card.component.css',
})
export class JobCardComponent {
  @Input({ required: true }) job!: JobCardModel;
  @Input() image = 'assets/web_css/img/post.png';
  @Input() detailRoute = '/single';

  private readonly savedJobsService = inject(SavedJobsService);
  private readonly i18nService = inject(I18nService);

  get isSaved(): boolean {
    if (!this.job || !this.job.id) return false;
    return this.savedJobsService.isSaved(this.job.id);
  }

  get companyName(): string {
    if (this.job?.companyName) return this.job.companyName;
    return 'Lumina Tech Solutions';
  }

  get formattedSalary(): string {
    const raw = this.job?.salary;
    const negotiable = this.i18nService.translate('category.card.negotiableSalary');
    const millionUnit = this.i18nService.translate('category.card.millionVnd');

    if (raw === undefined || raw === null || raw === '') {
      return negotiable;
    }

    if (typeof raw === 'number') {
      if (raw <= 0) return negotiable;
      if (raw >= 1000000) {
        const million = raw / 1000000;
        return `${million % 1 === 0 ? million : million.toFixed(1)} ${millionUnit}`;
      }
      return `${raw.toLocaleString('vi-VN')} đ`;
    }

    const str = String(raw).trim();
    const num = Number(str);
    if (!isNaN(num) && num > 0) {
      if (num >= 1000000) {
        const million = num / 1000000;
        return `${million % 1 === 0 ? million : million.toFixed(1)} ${millionUnit}`;
      }
      return `${num.toLocaleString('vi-VN')} đ`;
    }

    return str;
  }

  get formattedTime(): string {
    const time = this.job?.time;
    if (!time) {
      return this.i18nService.translate('category.card.employmentTypes.FULL_TIME');
    }
    const upper = String(time).toUpperCase();
    const key = `category.card.employmentTypes.${upper}`;
    const translated = this.i18nService.translate(key);
    return translated !== key ? translated : time;
  }

  get skillTags(): string[] {
    if (this.job?.skills && Array.isArray(this.job.skills) && this.job.skills.length > 0) {
      return this.job.skills.slice(0, 3);
    }

    // Smart skill tag extraction from job title
    const tags: string[] = [];
    const title = (this.job?.title || '').toLowerCase();

    const dictionary: Array<{ check: string[]; tag: string }> = [
      { check: ['java', 'spring'], tag: 'Java' },
      { check: ['react', 'nextjs', 'next.js'], tag: 'React' },
      { check: ['angular'], tag: 'Angular' },
      { check: ['vue', 'nuxt'], tag: 'Vue.js' },
      { check: ['node', 'express', 'nest'], tag: 'Node.js' },
      { check: ['python', 'django', 'fastapi'], tag: 'Python' },
      { check: ['golang', 'go developer'], tag: 'Golang' },
      { check: ['php', 'laravel'], tag: 'PHP / Laravel' },
      { check: ['.net', 'c#', 'csharp'], tag: '.NET / C#' },
      { check: ['frontend', 'front-end'], tag: 'Frontend' },
      { check: ['backend', 'back-end'], tag: 'Backend' },
      { check: ['fullstack', 'full-stack'], tag: 'Fullstack' },
      { check: ['devops', 'aws', 'cloud', 'docker', 'kubernetes'], tag: 'DevOps' },
      { check: ['mobile', 'flutter', 'react native', 'ios', 'android'], tag: 'Mobile' },
      { check: ['ui/ux', 'designer', 'figma'], tag: 'UI/UX Design' },
      { check: ['tester', 'qa', 'qc', 'automation'], tag: 'QA / Tester' },
      { check: ['data', 'analytics', 'sql', 'bi'], tag: 'SQL / Data' },
      { check: ['ai', 'machine learning', 'llm'], tag: 'AI / ML' },
      { check: ['marketing', 'seo', 'content'], tag: 'Marketing' },
      { check: ['sales', 'kinh doanh', 'bán hàng'], tag: 'Kinh doanh' },
      { check: ['kế toán', 'accounting'], tag: 'Kế toán' },
      { check: ['hr', 'nhân sự', 'recruiter'], tag: 'Nhân sự' },
    ];

    for (const item of dictionary) {
      if (item.check.some((c) => title.includes(c))) {
        tags.push(item.tag);
        if (tags.length >= 3) break;
      }
    }

    // Do NOT include redundant employment type as skill tag
    return tags;
  }

  toggleBookmark(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.job && this.job.id) {
      this.savedJobsService.toggleSave(this.job.id);
    }
  }
}
