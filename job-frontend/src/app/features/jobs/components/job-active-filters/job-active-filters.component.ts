import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { Category } from '../../../../shared/models/category.model';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import {
  EMPLOYMENT_TYPE_OPTIONS,
  EXPERIENCE_OPTIONS,
  LEVEL_OPTIONS,
  SALARY_OPTIONS,
  WORKPLACE_OPTIONS,
} from '../../models/job-filter.model';

export interface ActiveFilterChip {
  id: string;
  type: 'keyword' | 'category' | 'address' | 'employmentType' | 'salary' | 'experience' | 'level' | 'workplace';
  label: string;
  value: any;
}

@Component({
  selector: 'app-job-active-filters',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './job-active-filters.component.html',
  styleUrl: './job-active-filters.component.css',
})
export class JobActiveFiltersComponent {
  @Input() keyword = '';
  @Input() categoryIds: number[] = [];
  @Input() addresses: string[] = [];
  @Input() employmentTypes: string[] = [];
  @Input() salaryRange = 'all';
  @Input() experience = 'all';
  @Input() level = 'all';
  @Input() workplaces: string[] = [];
  @Input() allCategories: Category[] = [];

  @Output() removeChip = new EventEmitter<ActiveFilterChip>();
  @Output() clearAll = new EventEmitter<void>();

  private readonly i18nService = inject(I18nService);

  get chips(): ActiveFilterChip[] {
    const list: ActiveFilterChip[] = [];

    // Keyword
    if (this.keyword && this.keyword.trim().length > 0) {
      const prefix = this.i18nService.translate('category.activeFilters.keywordPrefix');
      list.push({
        id: `kw-${this.keyword}`,
        type: 'keyword',
        label: `${prefix}: "${this.keyword.trim()}"`,
        value: this.keyword,
      });
    }

    // Categories
    for (const catId of this.categoryIds) {
      const found = this.allCategories.find((c) => c.id === catId);
      const prefix = this.i18nService.translate('category.activeFilters.categoryPrefix');
      list.push({
        id: `cat-${catId}`,
        type: 'category',
        label: found ? found.name : `${prefix}: ${catId}`,
        value: catId,
      });
    }

    // Locations / Addresses
    for (const addr of this.addresses) {
      list.push({
        id: `addr-${addr}`,
        type: 'address',
        label: addr,
        value: addr,
      });
    }

    // Employment Types
    for (const et of this.employmentTypes) {
      const opt = EMPLOYMENT_TYPE_OPTIONS.find((o) => o.id === et);
      const translatedLabel = opt?.labelKey ? this.i18nService.translate(opt.labelKey) : (opt ? opt.label : et);
      list.push({
        id: `et-${et}`,
        type: 'employmentType',
        label: translatedLabel,
        value: et,
      });
    }

    // Workplace (Remote / Hybrid / Onsite)
    for (const wp of this.workplaces) {
      const opt = WORKPLACE_OPTIONS.find((o) => o.id === wp);
      const translatedLabel = opt?.labelKey ? this.i18nService.translate(opt.labelKey) : (opt ? opt.label : wp);
      list.push({
        id: `wp-${wp}`,
        type: 'workplace',
        label: translatedLabel,
        value: wp,
      });
    }

    // Salary
    if (this.salaryRange && this.salaryRange !== 'all') {
      const opt = SALARY_OPTIONS.find((o) => o.id === this.salaryRange);
      if (opt) {
        const prefix = this.i18nService.translate('category.activeFilters.salaryPrefix');
        const translatedLabel = opt.labelKey ? this.i18nService.translate(opt.labelKey) : opt.label;
        list.push({
          id: `sal-${this.salaryRange}`,
          type: 'salary',
          label: `${prefix}: ${translatedLabel}`,
          value: this.salaryRange,
        });
      }
    }

    // Experience
    if (this.experience && this.experience !== 'all') {
      const opt = EXPERIENCE_OPTIONS.find((o) => o.id === this.experience);
      if (opt) {
        const prefix = this.i18nService.translate('category.activeFilters.experiencePrefix');
        const translatedLabel = opt.labelKey ? this.i18nService.translate(opt.labelKey) : opt.label;
        list.push({
          id: `exp-${this.experience}`,
          type: 'experience',
          label: `${prefix}: ${translatedLabel}`,
          value: this.experience,
        });
      }
    }

    // Level
    if (this.level && this.level !== 'all') {
      const opt = LEVEL_OPTIONS.find((o) => o.id === this.level);
      if (opt) {
        const prefix = this.i18nService.translate('category.activeFilters.levelPrefix');
        const translatedLabel = opt.labelKey ? this.i18nService.translate(opt.labelKey) : opt.label;
        list.push({
          id: `lvl-${this.level}`,
          type: 'level',
          label: `${prefix}: ${translatedLabel}`,
          value: this.level,
        });
      }
    }

    return list;
  }

  get hasActiveFilters(): boolean {
    return this.chips.length > 0;
  }

  onRemove(chip: ActiveFilterChip): void {
    this.removeChip.emit(chip);
  }

  onClearAll(): void {
    this.clearAll.emit();
  }
}
