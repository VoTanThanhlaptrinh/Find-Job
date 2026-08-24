import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Category } from '../../../../shared/models/category.model';
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
  imports: [CommonModule],
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

  get chips(): ActiveFilterChip[] {
    const list: ActiveFilterChip[] = [];

    // Keyword
    if (this.keyword && this.keyword.trim().length > 0) {
      list.push({
        id: `kw-${this.keyword}`,
        type: 'keyword',
        label: `Từ khóa: "${this.keyword.trim()}"`,
        value: this.keyword,
      });
    }

    // Categories
    for (const catId of this.categoryIds) {
      const found = this.allCategories.find((c) => c.id === catId);
      list.push({
        id: `cat-${catId}`,
        type: 'category',
        label: found ? found.name : `Ngành: ${catId}`,
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
      list.push({
        id: `et-${et}`,
        type: 'employmentType',
        label: opt ? opt.label : et,
        value: et,
      });
    }

    // Workplace (Remote / Hybrid / Onsite)
    for (const wp of this.workplaces) {
      const opt = WORKPLACE_OPTIONS.find((o) => o.id === wp);
      list.push({
        id: `wp-${wp}`,
        type: 'workplace',
        label: opt ? opt.label : wp,
        value: wp,
      });
    }

    // Salary
    if (this.salaryRange && this.salaryRange !== 'all') {
      const opt = SALARY_OPTIONS.find((o) => o.id === this.salaryRange);
      if (opt) {
        list.push({
          id: `sal-${this.salaryRange}`,
          type: 'salary',
          label: `Lương: ${opt.label}`,
          value: this.salaryRange,
        });
      }
    }

    // Experience
    if (this.experience && this.experience !== 'all') {
      const opt = EXPERIENCE_OPTIONS.find((o) => o.id === this.experience);
      if (opt) {
        list.push({
          id: `exp-${this.experience}`,
          type: 'experience',
          label: `Kinh nghiệm: ${opt.label}`,
          value: this.experience,
        });
      }
    }

    // Level
    if (this.level && this.level !== 'all') {
      const opt = LEVEL_OPTIONS.find((o) => o.id === this.level);
      if (opt) {
        list.push({
          id: `lvl-${this.level}`,
          type: 'level',
          label: `Cấp bậc: ${opt.label}`,
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
