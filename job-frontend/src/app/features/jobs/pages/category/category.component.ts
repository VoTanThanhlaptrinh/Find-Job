import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged, Subject, take } from 'rxjs';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import {
  AddressCountViewModel,
  JobFilterPayload,
} from '../../../../shared/models/jobs/job-api-response.model';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-category',
  imports: [
    MatPaginatorModule,
    FormsModule,
    NgxSliderModule,
    JobCardComponent,
    SkeletonJobCardComponent,
    CallToActionComponent,
  ],
  standalone: true,
  templateUrl: './category.component.html',
  styleUrl: './category.component.css',
})
export class CategoryComponent implements OnInit {
  addressCount: AddressCountViewModel[] = [];
  pageIndex = 0;
  pageSize = 10;
  length = 0;
  min = 0;
  max = 100000000;
  step = 1000000;
  minGap = 2000000;

  minSalary = 5000000;
  maxSalary = 50000000;
  selectedAddresses = new Set<string>();
  selectedTypes = new Set<string>();
  jobTypes = [
    { label: 'Full time', checked: false },
    { label: 'Part time', checked: false },
    { label: 'Remote', checked: false },
    { label: 'Hybrid', checked: false },
  ];

  private searchSubject = new Subject<string>();
  title: string = '';
  constructor(private category: CategoryService) {
  }

  ngOnInit(): void {
    this.category.listJobsNewest(this.pageIndex, this.pageSize);
    this.getAmount();
    this.getAddressCount();
    this.addressCount = this.category.addressCount();
    this.searchSubject.pipe(
      debounceTime(400),        // Đợi 400ms sau khi ngừng gõ mới chạy tiếp
      distinctUntilChanged()    // Chỉ gọi API nếu giá trị thực sự thay đổi so với lần trước
    ).subscribe(searchValue => {
      this.executeSearch(searchValue);
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
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;

    if (this.hasActiveFilters()) {
      this.searchWithFilters(this.buildFilterPayload());
      return;
    }

    this.category.listJobsNewest(this.pageIndex, this.pageSize);
  }

  getAddressCount(): void {
    this.category.getAddressCount();
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

    if (this.addressCount.some((item) => item.city === value)) {
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
      title: this.title,
    };
  }

  private hasActiveFilters(): boolean {
    const hasAddress = this.selectedAddresses.size > 0;
    const hasType = this.selectedTypes.size > 0;
    const hasTitle = this.title.trim().length > 0;
    const hasSalaryRangeChange =
      this.minSalary !== 5000000 || this.maxSalary !== 50000000;

    return hasAddress || hasType || hasTitle || hasSalaryRangeChange;
  }

  private searchWithFilters(filter: JobFilterPayload): void {
    this.category.filterWithAddressTimeSalary(filter);
  }
  jobs() {
    return this.category.jobs();
  }

  isLoadingJobs() {
    return this.category.isLoadingJobs();
  }

  onTitleChange(value: string) {
    this.searchSubject.next(value);
  }

  private executeSearch(value: string) {
    this.title = value; // Cập nhật title chính thức
    this.pageIndex = 0; // Reset về trang đầu
    this.searchWithFilters(this.buildFilterPayload());
  }
}
