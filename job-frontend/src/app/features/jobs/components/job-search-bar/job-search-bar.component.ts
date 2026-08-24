import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategoryService } from '../../../../core/services/category.service';
import { FilterService } from '../../services/filter.service';
import { CustomSelectComponent, SelectOption } from '../../../../shared/components/custom-select/custom-select.component';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-job-search-bar',
  standalone: true,
  imports: [CommonModule, FormsModule, CustomSelectComponent, TranslatePipe],
  templateUrl: './job-search-bar.component.html',
  styleUrl: './job-search-bar.component.css',
})
export class JobSearchBarComponent {
  @Input() keyword = '';
  @Input() selectedCategoryId: number | '' = '';
  @Input() selectedAddress = '';

  @Output() search = new EventEmitter<{
    keyword: string;
    categoryId: number | '';
    address: string;
  }>();

  private readonly categoryService = inject(CategoryService);
  private readonly filterService = inject(FilterService);

  readonly categories = this.categoryService.categories;
  readonly addressCount = this.filterService.addressCount;

  get categoryOptions(): SelectOption<number>[] {
    return this.categories().map((c) => ({
      value: c.id,
      label: c.name,
      icon: 'category',
    }));
  }

  get locationOptions(): SelectOption<string>[] {
    return this.addressCount().map((a) => ({
      value: a.city,
      label: a.city,
      count: a.count,
      icon: 'location_on',
    }));
  }

  onCategorySelect(val: any): void {
    this.selectedCategoryId = val !== '' ? Number(val) : '';
  }

  onLocationSelect(val: any): void {
    this.selectedAddress = val || '';
  }

  onSearchSubmit(): void {
    this.search.emit({
      keyword: this.keyword.trim(),
      categoryId: this.selectedCategoryId !== '' ? Number(this.selectedCategoryId) : '',
      address: this.selectedAddress.trim(),
    });
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.onSearchSubmit();
    }
  }
}
