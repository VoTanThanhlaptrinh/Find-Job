import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, effect, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject, take } from 'rxjs';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import {
  AddressCountViewModel,
  JobFilterPayload,
} from '../../../../shared/models/jobs/job-api-response.model';
import { CategoryService } from '../../services/category.service';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-category',
  imports: [
    FormsModule,
    NgxSliderModule,
    JobCardComponent,
    SkeletonJobCardComponent,
    CallToActionComponent,
    TranslatePipe,
  ],
  standalone: true,
  templateUrl: './category.component.html',
  styleUrl: './category.component.css',
})
export class CategoryComponent implements OnInit, AfterViewInit {
  readonly pageSizeOptions = [5, 10, 25];
  addressCount: AddressCountViewModel[] = [];
  jobs: JobCardModel[] = [];
  pageIndex = 0;
  pageSize = 5;
  length = 0;
  selectedAddresses = new Set<string>();
  selectedTypes = new Set<string>();
  jobTypes = [
    { id: 'FULL_TIME', value: 'FULL_TIME', checked: false },
    { id: 'PART_TIME', value: 'PART_TIME', checked: false },
    { id: 'REMOTE', value: 'REMOTE', checked: false },
    { id: 'HYBRID', value: 'HYBRID', checked: false },
  ];

  private searchSubject = new Subject<string>();
  private readonly platformId = inject(PLATFORM_ID);
  private readonly scrollToTopThreshold = 24;
  title: string = '';
  constructor(
    private category: CategoryService,
    private i18nService: I18nService,
    private route: ActivatedRoute,
  ) {
    effect(() => {
      this.addressCount = this.category.addressCount();
      this.jobs = this.category.jobs();
      const totalJobs = this.category.totalJobs();

      if (totalJobs !== null) {
        this.length = totalJobs;
      }
    });
  }

  ngOnInit(): void {
    this.getAddressCount();
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(searchValue => {
      this.executeSearch(searchValue);
    });

    if (this.applyInitialQueryParams()) {
      this.searchWithFilters(this.buildFilterPayload());
      return;
    }

    this.category.listJobsNewest(this.pageIndex, this.pageSize);
    this.getAmount();
  }

  ngAfterViewInit(): void {
    this.scrollToTopIfNeeded();
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
    this.scrollToTopIfNeeded();

    if (this.hasActiveFilters()) {
      this.searchWithFilters(this.buildFilterPayload());
      return;
    }

    this.category.listJobsNewest(this.pageIndex, this.pageSize);
  }

  getAddressCount(): void {
    this.category.loadAddressCount();
  }

  formatMoney(value: number): string {
    const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
    return `${value.toLocaleString(locale)} VND`;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.length / this.pageSize));
  }

  get visiblePageItems(): Array<number | 'ellipsis'> {
    const totalPages = this.totalPages;

    if (totalPages <= 5) {
      return Array.from({ length: totalPages }, (_, index) => index + 1);
    }

    const currentPage = this.pageIndex + 1;
    const pages = new Set<number>([1, totalPages, currentPage, currentPage - 1, currentPage + 1]);
    const sortedPages = Array.from(pages)
      .filter((page) => page >= 1 && page <= totalPages)
      .sort((left, right) => left - right);

    const visibleItems: Array<number | 'ellipsis'> = [];

    sortedPages.forEach((page, index) => {
      if (index > 0 && page - sortedPages[index - 1] > 1) {
        visibleItems.push('ellipsis');
      }

      visibleItems.push(page);
    });

    return visibleItems;
  }

  get paginationStart(): number {
    if (this.length === 0) {
      return 0;
    }

    return this.pageIndex * this.pageSize + 1;
  }

  get paginationEnd(): number {
    return Math.min((this.pageIndex + 1) * this.pageSize, this.length);
  }

  get rowsPerPageLabel(): string {
    return this.i18nService.currentLanguage === 'vi' ? 'Hien thi moi trang' : 'Rows per page';
  }

  get showingLabel(): string {
    return this.i18nService.currentLanguage === 'vi' ? 'Hien thi' : 'Showing';
  }

  get ofLabel(): string {
    return this.i18nService.currentLanguage === 'vi' ? 'tren' : 'of';
  }

  get previousLabel(): string {
    return this.i18nService.currentLanguage === 'vi' ? 'Truoc' : 'Previous';
  }

  get nextLabel(): string {
    return this.i18nService.currentLanguage === 'vi' ? 'Sau' : 'Next';
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

    if (this.jobTypes.some((item) => item.value === value)) {
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
      address: Array.from(this.selectedAddresses),
      times: Array.from(this.selectedTypes),
      title: this.title.trim(),
    };
  }

  private hasActiveFilters(): boolean {
    const hasAddress = this.selectedAddresses.size > 0;
    const hasType = this.selectedTypes.size > 0;
    const hasTitle = this.title.trim().length > 0;

    return hasAddress || hasType || hasTitle;
  }

  private searchWithFilters(filter: JobFilterPayload): void {
    this.category.filterWithAddressTimeSalary(filter);
  }
  isLoadingJobs() {
    return this.category.isLoadingJobs();
  }

  onTitleChange(value: string) {
    this.searchSubject.next(value);
  }

  onPageSizeChange(value: string): void {
    const nextPageSize = Number(value);

    if (!this.pageSizeOptions.includes(nextPageSize) || nextPageSize === this.pageSize) {
      return;
    }

    this.handlePage({
      pageIndex: 0,
      previousPageIndex: this.pageIndex,
      pageSize: nextPageSize,
      length: this.length,
    });
  }

  goToPage(pageNumber: number): void {
    const targetPageIndex = pageNumber - 1;

    if (targetPageIndex < 0 || targetPageIndex >= this.totalPages || targetPageIndex === this.pageIndex) {
      return;
    }

    this.handlePage({
      pageIndex: targetPageIndex,
      previousPageIndex: this.pageIndex,
      pageSize: this.pageSize,
      length: this.length,
    });
  }

  goToPreviousPage(): void {
    this.goToPage(this.pageIndex);
  }

  goToNextPage(): void {
    this.goToPage(this.pageIndex + 2);
  }

  isCurrentPage(pageItem: number | 'ellipsis'): boolean {
    return typeof pageItem === 'number' && pageItem === this.pageIndex + 1;
  }

  private executeSearch(value: string) {
    this.title = value;
    this.pageIndex = 0;
    this.searchWithFilters(this.buildFilterPayload());
  }

  private applyInitialQueryParams(): boolean {
    const keyword = this.route.snapshot.queryParamMap.get('keyword')?.trim() ?? '';
    const city = this.route.snapshot.queryParamMap.get('city')?.trim() ?? '';

    this.title = keyword;
    this.selectedAddresses.clear();

    if (city.length > 0) {
      this.selectedAddresses.add(city);
    }

    return keyword.length > 0 || city.length > 0;
  }

  private scrollToTopIfNeeded(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    requestAnimationFrame(() => {
      const currentScrollY = window.scrollY || window.pageYOffset || 0;

      if (currentScrollY <= this.scrollToTopThreshold) {
        return;
      }

      window.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
    });
  }
}
