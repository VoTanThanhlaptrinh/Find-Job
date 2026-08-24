import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { AddressCountViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { Category } from '../../../../shared/models/category.model';
import {
  EMPLOYMENT_TYPE_OPTIONS,
  EXPERIENCE_OPTIONS,
  LEVEL_OPTIONS,
  SALARY_OPTIONS,
  WORKPLACE_OPTIONS,
} from '../../models/job-filter.model';

import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-job-mobile-filter-drawer',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './job-mobile-filter-drawer.component.html',
  styleUrl: './job-mobile-filter-drawer.component.css',
})
export class JobMobileFilterDrawerComponent {
  @Input() isOpen = false;
  @Input() totalJobs = 0;
  @Input() categories: Category[] = [];
  @Input() addressCount: AddressCountViewModel[] = [];
  @Input() selectedCategoryIds: number[] = [];
  @Input() selectedAddresses: string[] = [];
  @Input() selectedSalaryRange = 'all';
  @Input() selectedExperience = 'all';
  @Input() selectedLevel = 'all';
  @Input() selectedEmploymentTypes: string[] = [];
  @Input() selectedWorkplaces: string[] = [];
  @Input() activeFiltersCount = 0;

  @Output() closeDrawer = new EventEmitter<void>();
  @Output() categoryChange = new EventEmitter<{ id: number; checked: boolean }>();
  @Output() addressChange = new EventEmitter<{ city: string; checked: boolean }>();
  @Output() salaryChange = new EventEmitter<string>();
  @Output() experienceChange = new EventEmitter<string>();
  @Output() levelChange = new EventEmitter<string>();
  @Output() employmentTypeChange = new EventEmitter<{ type: string; checked: boolean }>();
  @Output() workplaceChange = new EventEmitter<{ workplace: string; checked: boolean }>();
  @Output() clearAll = new EventEmitter<void>();
  @Output() apply = new EventEmitter<void>();

  readonly salaryOptions = SALARY_OPTIONS;
  readonly experienceOptions = EXPERIENCE_OPTIONS;
  readonly levelOptions = LEVEL_OPTIONS;
  readonly employmentTypeOptions = EMPLOYMENT_TYPE_OPTIONS;
  readonly workplaceOptions = WORKPLACE_OPTIONS;

  expandedGroups: Record<string, boolean> = {
    category: true,
    location: true,
    salary: true,
    experience: false,
    level: false,
    employmentType: false,
    workplace: false,
  };

  showAllCategories = false;
  showAllLocations = false;

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.isOpen) {
      this.onClose();
    }
  }

  toggleGroup(groupId: string): void {
    this.expandedGroups[groupId] = !this.expandedGroups[groupId];
  }

  isCategorySelected(id: number): boolean {
    return this.selectedCategoryIds.includes(id);
  }

  isAddressSelected(city: string): boolean {
    return this.selectedAddresses.includes(city);
  }

  isEmploymentTypeSelected(type: string): boolean {
    return this.selectedEmploymentTypes.includes(type);
  }

  isWorkplaceSelected(wp: string): boolean {
    return this.selectedWorkplaces.includes(wp);
  }

  onCategoryToggle(id: number, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.categoryChange.emit({ id, checked });
  }

  onAddressToggle(city: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.addressChange.emit({ city, checked });
  }

  onSalarySelect(salaryId: string): void {
    this.salaryChange.emit(salaryId);
  }

  onExperienceSelect(expId: string): void {
    this.experienceChange.emit(expId);
  }

  onLevelSelect(lvlId: string): void {
    this.levelChange.emit(lvlId);
  }

  onEmploymentTypeToggle(type: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.employmentTypeChange.emit({ type, checked });
  }

  onWorkplaceToggle(workplace: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.workplaceChange.emit({ workplace, checked });
  }

  onClose(): void {
    this.closeDrawer.emit();
  }

  onClearAll(): void {
    this.clearAll.emit();
  }

  onApply(): void {
    this.apply.emit();
    this.onClose();
  }
}
