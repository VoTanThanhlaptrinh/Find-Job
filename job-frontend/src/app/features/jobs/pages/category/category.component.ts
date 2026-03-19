import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSlider, MatSliderRangeThumb } from '@angular/material/slider';
import { take } from 'rxjs';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SearchFormComponent } from '../../../../shared/components/search-form/search-form.component';
import {
  AddressCountViewModel,
  JobFilterPayload,
} from '../../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-category',
  imports: [
    MatPaginatorModule,
    FormsModule,
    NgxSliderModule,
    MatSlider,
    MatSliderRangeThumb,
    SearchFormComponent,
    JobCardComponent,
    CallToActionComponent,
  ],
  standalone: true,
  templateUrl: './category.component.html',
  styleUrl: './category.component.css',
})
export class CategoryComponent implements OnInit {
  listJobsNewest: JobCardModel[] = [];
  addressCount: AddressCountViewModel[] = [];

  pageIndex = 0;
  pageSize = 10;
  length = 0;
  min = 0;
  max = 100000000;
  step = 500000;
  minGap = 2000000;

  minSalary = 5000000;
  maxSalary = 50000000;
  selectedAddresses = new Set<string>();
  selectedTypes = new Set<string>();
  jobTypes = [
    { label: 'Full time', checked: false },
    { label: 'Part time', checked: false },
    { label: 'Freelance', checked: false },
    { label: 'Internship', checked: false },
  ];

  constructor(private category: CategoryService) {}

  ngOnInit(): void {
    this.getListJobsNewest(this.pageIndex, this.pageSize);
    this.getAmount();
    this.getAddressCount();
  }

  getListJobsNewest(pageIndex: number, pageSize: number): void {
    this.category.listJobsNewest(pageIndex, pageSize).subscribe({
      next: (res) => {
        this.listJobsNewest = res.data.content;
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }

  getAmount(): void {
    this.category.getAmount().pipe(take(1)).subscribe({
      next: (response) => {
        this.length = response.data;
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }

  handlePage(event: PageEvent): void {
    this.getListJobsNewest(event.pageIndex, event.pageSize);
  }

  getAddressCount(): void {
    this.category.getAddressCount().subscribe({
      next: (res) => {
        this.addressCount = res.data;
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }

  formatMoney(value: number): string {
    return `${value.toLocaleString('vi-VN')} VND`;
  }

  onMinChange(value: number): void {
    this.minSalary = Math.min(value, this.maxSalary - this.minGap);
    this.searchWithFilters(this.buildFilterPayload());
  }

  onMaxChange(value: number): void {
    this.maxSalary = Math.max(value, this.minSalary + this.minGap);
    this.searchWithFilters(this.buildFilterPayload());
  }

  onFilterChange(event: Event): void {
    const input = event.target as HTMLInputElement | null;

    if (!input) {
      return;
    }

    const value = input.value;
    const checked = input.checked;

    if (this.addressCount.some((item) => item.address === value)) {
      if (checked) {
        this.selectedAddresses.add(value);
      } else {
        this.selectedAddresses.delete(value);
      }
    }

    if (this.jobTypes.some((item) => item.label === value)) {
      if (checked) {
        this.selectedTypes.add(value);
      } else {
        this.selectedTypes.delete(value);
      }
    }

    this.pageIndex = 0;
    this.searchWithFilters(this.buildFilterPayload());
  }

  private buildFilterPayload(): JobFilterPayload {
    return {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      min: this.minSalary,
      max: this.maxSalary,
      address: Array.from(this.selectedAddresses),
      times: Array.from(this.selectedTypes),
    };
  }

  private searchWithFilters(filter: JobFilterPayload): void {
    this.category.filterWithAddressTimeSalary(filter).subscribe({
      next: (res) => {
        this.listJobsNewest = res.data.content;
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      },
    });
  }
}
