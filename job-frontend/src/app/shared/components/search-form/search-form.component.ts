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
  }

  onSubmit(): void {
    const keyword = this.keyword.trim();
    const city = this.selectedCity.trim();
    const queryParams: { keyword?: string; city?: string } = {};

    if (keyword.length > 0) {
      queryParams.keyword = keyword;
    }

    if (city.length > 0) {
      queryParams.city = city;
    }

    this.router.navigate(['/category'], { queryParams });
  }
}
