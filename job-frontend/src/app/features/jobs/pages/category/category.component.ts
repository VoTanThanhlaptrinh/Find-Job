import { isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, DestroyRef, effect, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged, Subject, take } from 'rxjs';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import {
  AddressCountViewModel,
  JobFilterPayload,
} from '../../../../shared/models/jobs/job-api-response.model';
import { CategoryService } from '../../../../core/services/category.service';
import { FilterService } from '../../services/filter.service';
import { Category } from '../../../../shared/models/category.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  private readonly destroyRef = inject(DestroyRef);
  addressCount: AddressCountViewModel[] = [];
  jobs: JobCardModel[] = [];
  categories: Category[] = [];
  pageIndex = 0;
  pageSize = 5;
  length = 0;
  selectedAddresses = new Set<string>();
  selectedTypes = new Set<string>();
  selectedCategories = new Set<number>();
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
  isLoadingJobs = false;

  constructor(
    private category: CategoryService,
    private filterService: FilterService,
    private i18nService: I18nService,
  ) {
    effect(() => {
      this.addressCount = this.filterService.addressCount();
      this.jobs = this.filterService.jobs();
      this.categories = this.category.categories();
      this.isLoadingJobs = this.filterService.isLoadingJobs();

      const total = this.filterService.totalJobs();
      this.length = total !== null ? total : 0;
    });
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(searchValue => {
      this.executeSearch(searchValue);
    });
  }

  ngOnInit(): void {
    this.getAddressCount();
    this.category.loadCategories();
    this.applyFilterState(this.filterService.getFilterSnapshot());
    this.fetchJobs();
  }

  ngAfterViewInit(): void {
    this.scrollToTopIfNeeded();
  }

  handlePage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.scrollToTopIfNeeded();
    this.fetchJobs();
  }

  getAddressCount(): void {
    this.filterService.loadAddressCount();
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

  onFilterChange(filterType: 'ADDRESS' | 'JOB_TYPE' | 'CATEGORY', value: any, isChecked: boolean): void {
    if (filterType === 'ADDRESS') {
      isChecked ? this.selectedAddresses.add(value) : this.selectedAddresses.delete(value);
    }
    else if (filterType === 'JOB_TYPE') {
      isChecked ? this.selectedTypes.add(value) : this.selectedTypes.delete(value);

      const jobTypeObj = this.jobTypes.find(item => item.value === value);
      if (jobTypeObj) {
        jobTypeObj.checked = isChecked;
      }
    }
    else if (filterType === 'CATEGORY') {
      isChecked ? this.selectedCategories.add(value) : this.selectedCategories.delete(value);
    }

    this.pageIndex = 0;
    this.fetchJobs();
  }

  private buildFilterPayload(): JobFilterPayload {
    return {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      address: this.selectedAddresses ? Array.from(this.selectedAddresses) : [],
      times: this.selectedTypes ? Array.from(this.selectedTypes) : [],
      title: this.title ? this.title.trim() : '',
      categoryIds: this.selectedCategories ? Array.from(this.selectedCategories) : [],
    };
  }

  private hasActiveFilters(): boolean {
    const hasAddress = this.selectedAddresses ? this.selectedAddresses.size > 0 : false;
    const hasType = this.selectedTypes ? this.selectedTypes.size > 0 : false;
    const hasTitle = this.title ? this.title.trim().length > 0 : false;
    const hasCategory = this.selectedCategories ? this.selectedCategories.size > 0 : false;

    return hasAddress || hasType || hasTitle || hasCategory;
  }

  private searchWithFilters(filter: JobFilterPayload): void {
    this.filterService.filterWithAddressTimeSalary(filter);
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
    this.title = value || '';
    this.pageIndex = 0;
    this.fetchJobs();
  }

  private applyFilterState(filter: JobFilterPayload | null | undefined): void {
    if (!filter) return;

    this.pageIndex = filter.pageIndex ?? 0;
    this.pageSize = filter.pageSize ?? 5;
    this.title = filter.title ?? '';
    this.selectedAddresses = new Set(filter.address || []);
    this.selectedTypes = new Set(filter.times || []);
    this.selectedCategories = new Set(filter.categoryIds || []);

    this.jobTypes.forEach((job) => {
      job.checked = this.selectedTypes.has(job.value);
    });
  }
  private fetchJobs(): void {
    if (this.hasActiveFilters()) {
      this.searchWithFilters(this.buildFilterPayload());
    }
    else {
      this.filterService.resetFilterPayload({
        pageIndex: this.pageIndex,
        pageSize: this.pageSize,
      });
      this.filterService.listJobsNewest(this.pageIndex, this.pageSize);
    }
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
