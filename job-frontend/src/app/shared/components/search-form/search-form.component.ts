import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FilterService } from '../../../features/jobs/services/filter.service';
import { CategoryService } from '../../../core/services/category.service';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { CustomSelectComponent, SelectOption } from '../custom-select/custom-select.component';

@Component({
  selector: 'app-search-form',
  imports: [FormsModule, TranslatePipe, CustomSelectComponent],
  templateUrl: './search-form.component.html',
  styleUrl: './search-form.component.css',
})
export class SearchFormComponent implements OnInit {
  keyword = '';
  selectedCity = '';
  selectedCategory: number | '' = '';
  private readonly router = inject(Router);
  private readonly filterService = inject(FilterService);
  private readonly categoryService = inject(CategoryService);

  readonly addressCount = this.filterService.addressCount;
  readonly categories = this.categoryService.categories;

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

  ngOnInit(): void {
    this.filterService.loadAddressCount();
    this.categoryService.loadCategories();
    const filter = this.filterService.getFilterSnapshot();
    this.keyword = filter.title;
    this.selectedCity = filter.address[0] ?? '';
    this.selectedCategory = filter.categoryIds?.[0] ?? '';
  }

  onCategorySelect(val: any): void {
    this.selectedCategory = val !== '' ? Number(val) : '';
  }

  onCitySelect(val: any): void {
    this.selectedCity = val || '';
  }

  get isSearchDisabled(): boolean {
    return !this.keyword.trim() && !this.selectedCity.trim() && this.selectedCategory === '';
  }

  onSubmit(): void {
    const keyword = this.keyword.trim();
    const city = this.selectedCity.trim();
    const queryParams: Record<string, any> = {};

    if (keyword) {
      queryParams['keyword'] = keyword;
    }
    if (city) {
      queryParams['location'] = city;
    }
    if (this.selectedCategory !== '') {
      queryParams['category'] = this.selectedCategory;
    }

    this.router.navigate(['/jobs'], { queryParams });
  }
}
