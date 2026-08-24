import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { SORT_OPTIONS } from '../../models/job-filter.model';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { CustomSelectComponent, SelectOption } from '../../../../shared/components/custom-select/custom-select.component';

@Component({
  selector: 'app-job-result-header',
  standalone: true,
  imports: [CommonModule, CustomSelectComponent, TranslatePipe],
  templateUrl: './job-result-header.component.html',
  styleUrl: './job-result-header.component.css',
})
export class JobResultHeaderComponent {
  @Input() totalJobs = 0;
  @Input() keyword = '';
  @Input() currentSort = 'relevant';
  @Input() activeFiltersCount = 0;

  @Output() sortChange = new EventEmitter<string>();
  @Output() openMobileFilter = new EventEmitter<void>();

  private readonly i18nService = inject(I18nService);

  get sortSelectOptions(): SelectOption<string>[] {
    return SORT_OPTIONS.map((opt) => ({
      value: opt.id,
      label: opt.labelKey ? this.i18nService.translate(opt.labelKey) : opt.label,
    }));
  }

  onSortChange(value: string): void {
    this.sortChange.emit(value);
  }

  onToggleMobileFilter(): void {
    this.openMobileFilter.emit();
  }
}
