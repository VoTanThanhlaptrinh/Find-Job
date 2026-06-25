import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { FilterService } from '../../../features/jobs/services/filter.service';
import { CategoryService } from '../../../core/services/category.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-search-form',
  imports: [FormsModule, TranslatePipe],
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

  ngOnInit(): void {
    this.filterService.loadAddressCount();
    this.categoryService.loadCategories();
    const filter = this.filterService.getFilterSnapshot();
    this.keyword = filter.title;
    this.selectedCity = filter.address[0] ?? '';
    this.selectedCategory = filter.categoryIds?.[0] ?? '';
  }

  get isSearchDisabled(): boolean {
    return !this.keyword.trim() && !this.selectedCity.trim() && this.selectedCategory === '';
  }

  onSubmit(): void {
    const keyword = this.keyword.trim();
    const city = this.selectedCity.trim();
    const currentFilter = this.filterService.getFilterSnapshot();

    this.filterService.setFilterPayload({
      ...currentFilter,
      pageIndex: 0,
      address: city.length > 0 ? [city] : [],
      categoryIds: this.selectedCategory !== '' ? [Number(this.selectedCategory)] : [],
      times: [],
      title: keyword,
    });

    this.router.navigate(['/category']);
  }
}
