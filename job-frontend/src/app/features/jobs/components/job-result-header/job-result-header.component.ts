import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SORT_OPTIONS } from '../../models/job-filter.model';
import { CustomSelectComponent, SelectOption } from '../../../../shared/components/custom-select/custom-select.component';

@Component({
  selector: 'app-job-result-header',
  standalone: true,
  imports: [CommonModule, CustomSelectComponent],
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

  get sortSelectOptions(): SelectOption<string>[] {
    return SORT_OPTIONS.map((opt) => ({
      value: opt.id,
      label: opt.label,
    }));
  }

  onSortChange(value: string): void {
    this.sortChange.emit(value);
  }

  onToggleMobileFilter(): void {
    this.openMobileFilter.emit();
  }
}
