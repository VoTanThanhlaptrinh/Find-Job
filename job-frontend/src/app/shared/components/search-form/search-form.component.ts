import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CategoryService } from '../../../features/jobs/services/category.service';
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
  private readonly router = inject(Router);
  private readonly categoryService = inject(CategoryService);
  readonly addressCount = this.categoryService.addressCount;

  ngOnInit(): void {
    this.categoryService.loadAddressCount();
    const filter = this.categoryService.getFilterSnapshot();
    this.keyword = filter.title;
    this.selectedCity = filter.address[0] ?? '';
  }

  onSubmit(): void {
    const keyword = this.keyword.trim();
    const city = this.selectedCity.trim();
    const currentFilter = this.categoryService.getFilterSnapshot();

    this.categoryService.setFilterPayload({
      ...currentFilter,
      pageIndex: 0,
      address: city.length > 0 ? [city] : [],
      times: [],
      title: keyword,
    });

    this.router.navigate(['/category']);
  }
}
