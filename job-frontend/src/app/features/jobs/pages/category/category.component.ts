import { CommonModule, isPlatformBrowser } from '@angular/common';
import { AfterViewInit, Component, DestroyRef, effect, inject, OnInit, PLATFORM_ID } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { CategoryService } from '../../../../core/services/category.service';
import { FilterService } from '../../services/filter.service';
import { Category } from '../../../../shared/models/category.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { AddressCountViewModel, JobFilterPayload } from '../../../../shared/models/jobs/job-api-response.model';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';
import { JobSearchBarComponent } from '../../components/job-search-bar/job-search-bar.component';
import { ActiveFilterChip, JobActiveFiltersComponent } from '../../components/job-active-filters/job-active-filters.component';
import { JobFilterSidebarComponent } from '../../components/job-filter-sidebar/job-filter-sidebar.component';
import { JobResultHeaderComponent } from '../../components/job-result-header/job-result-header.component';
import { JobPaginationComponent } from '../../components/job-pagination/job-pagination.component';
import { JobMobileFilterDrawerComponent } from '../../components/job-mobile-filter-drawer/job-mobile-filter-drawer.component';

import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-category',
  standalone: true,
  imports: [
    CommonModule,
    TranslatePipe,
    JobSearchBarComponent,
    JobActiveFiltersComponent,
    JobFilterSidebarComponent,
    JobResultHeaderComponent,
    JobCardComponent,
    SkeletonJobCardComponent,
    JobPaginationComponent,
    JobMobileFilterDrawerComponent,
    CallToActionComponent,
  ],
  templateUrl: './category.component.html',
  styleUrl: './category.component.css',
})
export class CategoryComponent implements OnInit, AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly categoryService = inject(CategoryService);
  private readonly filterService = inject(FilterService);

  // Data signals and properties
  jobs: JobCardModel[] = [];
  categories: Category[] = [];
  addressCount: AddressCountViewModel[] = [];
  totalJobs = 0;
  isLoadingJobs = false;
  hasError = false;

  // Filter & Search states
  keyword = '';
  categoryIds: number[] = [];
  addresses: string[] = [];
  employmentTypes: string[] = [];
  salaryRange = 'all';
  experience = 'all';
  level = 'all';
  workplaces: string[] = [];
  sort = 'relevant';
  pageIndex = 0;
  pageSize = 10;

  // Mobile drawer state
  isMobileDrawerOpen = false;

  private isInitializingFromUrl = true;

  constructor() {
    effect(() => {
      this.jobs = this.filterService.jobs();
      this.categories = this.categoryService.categories();
      this.addressCount = this.filterService.addressCount();
      this.isLoadingJobs = this.filterService.isLoadingJobs();
      this.hasError = this.filterService.hasError();

      const total = this.filterService.totalJobs();
      this.totalJobs = total !== null ? total : 0;
    });
  }

  ngOnInit(): void {
    this.categoryService.loadCategories();
    this.filterService.loadAddressCount();

    // Listen to query parameters for bidirectional URL sync
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        this.parseQueryParams(params);
        this.fetchJobs();
      });
  }

  ngAfterViewInit(): void {
    // Initial view initialization if needed
  }

  get totalActiveFiltersCount(): number {
    let count = 0;
    if (this.keyword && this.keyword.trim().length > 0) count++;
    count += this.categoryIds.length;
    count += this.addresses.length;
    count += this.employmentTypes.length;
    count += this.workplaces.length;
    if (this.salaryRange && this.salaryRange !== 'all') count++;
    if (this.experience && this.experience !== 'all') count++;
    if (this.level && this.level !== 'all') count++;
    return count;
  }

  get selectedCategoryId(): number | '' {
    return this.categoryIds.length > 0 ? this.categoryIds[0] : '';
  }

  get selectedAddress(): string {
    return this.addresses.length > 0 ? this.addresses[0] : '';
  }

  private parseQueryParams(params: Record<string, any>): void {
    // 1. Keyword
    this.keyword = (params['keyword'] || params['title'] || params['q'] || '').trim();

    // 2. Category
    const catParam = params['category'] || params['categoryId'] || params['categoryIds'];
    if (catParam) {
      if (typeof catParam === 'string') {
        this.categoryIds = catParam
          .split(',')
          .map((id) => Number(id.trim()))
          .filter((id) => !isNaN(id) && id > 0);
      } else if (typeof catParam === 'number') {
        this.categoryIds = [catParam];
      } else if (Array.isArray(catParam)) {
        this.categoryIds = catParam.map(Number).filter((id) => !isNaN(id) && id > 0);
      }
    } else {
      this.categoryIds = [];
    }

    // 3. Location / Address
    const locParam = params['location'] || params['address'] || params['city'];
    if (locParam) {
      if (typeof locParam === 'string') {
        this.addresses = locParam
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      } else if (Array.isArray(locParam)) {
        this.addresses = locParam.map((s) => String(s).trim()).filter(Boolean);
      }
    } else {
      this.addresses = [];
    }

    // 4. Work Type / Employment Type
    const workTypeParam = params['workType'] || params['employmentType'] || params['times'];
    if (workTypeParam) {
      if (workTypeParam === 'remote-hybrid') {
        this.workplaces = Array.from(new Set([...this.workplaces, 'REMOTE', 'HYBRID']));
        this.employmentTypes = Array.from(new Set([...this.employmentTypes, 'REMOTE', 'HYBRID']));
      } else if (typeof workTypeParam === 'string') {
        const types = workTypeParam
          .split(',')
          .map((s) => s.trim().toUpperCase())
          .filter(Boolean);
        this.employmentTypes = types;
      } else if (Array.isArray(workTypeParam)) {
        this.employmentTypes = workTypeParam.map((s) => String(s).trim().toUpperCase()).filter(Boolean);
      }
    } else {
      this.employmentTypes = [];
    }

    // 5. Workplace (Remote, Hybrid, Onsite)
    const wpParam = params['workplace'];
    if (wpParam) {
      if (typeof wpParam === 'string') {
        this.workplaces = wpParam
          .split(',')
          .map((s) => s.trim().toUpperCase())
          .filter(Boolean);
      } else if (Array.isArray(wpParam)) {
        this.workplaces = wpParam.map((s) => String(s).trim().toUpperCase()).filter(Boolean);
      }
    } else if (workTypeParam !== 'remote-hybrid') {
      this.workplaces = [];
    }

    // 6. Salary Range
    this.salaryRange = params['salary'] || params['salaryRange'] || 'all';

    // 7. Experience
    this.experience = params['experience'] || params['exp'] || 'all';

    // 8. Level
    this.level = params['level'] || 'all';
    if (this.level === 'intern-fresher' && !this.keyword) {
      // Keep intern-fresher level
    }

    // 9. Sort
    this.sort = params['sort'] || 'relevant';

    // 10. Page & Size
    const pageNum = Number(params['page']);
    this.pageIndex = !isNaN(pageNum) && pageNum > 0 ? pageNum - 1 : 0;

    const sizeNum = Number(params['size'] || params['pageSize']);
    this.pageSize = !isNaN(sizeNum) && sizeNum > 0 ? sizeNum : 10;
  }

  private updateUrlQueryParams(resetPage = false): void {
    if (resetPage) {
      this.pageIndex = 0;
    }

    const queryParams: Record<string, any> = {};

    if (this.keyword.trim().length > 0) {
      queryParams['keyword'] = this.keyword.trim();
    }

    if (this.categoryIds.length > 0) {
      queryParams['category'] = this.categoryIds.join(',');
    }

    if (this.addresses.length > 0) {
      queryParams['location'] = this.addresses.join(',');
    }

    if (this.employmentTypes.length > 0) {
      queryParams['workType'] = this.employmentTypes.join(',');
    }

    if (this.workplaces.length > 0) {
      queryParams['workplace'] = this.workplaces.join(',');
    }

    if (this.salaryRange && this.salaryRange !== 'all') {
      queryParams['salary'] = this.salaryRange;
    }

    if (this.experience && this.experience !== 'all') {
      queryParams['experience'] = this.experience;
    }

    if (this.level && this.level !== 'all') {
      queryParams['level'] = this.level;
    }

    if (this.sort && this.sort !== 'relevant') {
      queryParams['sort'] = this.sort;
    }

    if (this.pageIndex > 0) {
      queryParams['page'] = this.pageIndex + 1;
    }

    if (this.pageSize !== 10) {
      queryParams['size'] = this.pageSize;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: '', // Cleanly sync all params
    });
  }

  fetchJobs(): void {
    const hasActive = this.hasActiveFilterPayload();

    if (hasActive) {
      const payload = this.buildFilterPayload();
      this.filterService.filterWithAddressTimeSalary(payload, this.sort);
    } else {
      this.filterService.resetFilterPayload({
        pageIndex: this.pageIndex,
        pageSize: this.pageSize,
      });
      this.filterService.listJobsNewest(this.pageIndex, this.pageSize, this.sort);
    }
  }

  private hasActiveFilterPayload(): boolean {
    const hasKeyword = this.keyword.trim().length > 0;
    const hasCategory = this.categoryIds.length > 0;
    const hasAddress = this.addresses.length > 0;
    const hasType = this.employmentTypes.length > 0 || this.workplaces.length > 0;
    const hasSalary = this.salaryRange !== 'all';
    const hasExp = this.experience !== 'all';
    const hasLvl = this.level !== 'all';

    return hasKeyword || hasCategory || hasAddress || hasType || hasSalary || hasExp || hasLvl;
  }

  private buildFilterPayload(): JobFilterPayload {
    // Combine employment types & workplace types that map to backend EmploymentType
    const validBackendTypes = new Set<string>();

    for (const t of this.employmentTypes) {
      if (['FULL_TIME', 'PART_TIME', 'REMOTE', 'HYBRID'].includes(t)) {
        validBackendTypes.add(t);
      }
    }

    for (const w of this.workplaces) {
      if (['REMOTE', 'HYBRID'].includes(w)) {
        validBackendTypes.add(w);
      }
    }

    // Build title search string
    let searchTitle = this.keyword.trim();
    if (!searchTitle && this.level === 'intern-fresher') {
      searchTitle = 'Intern';
    }

    return {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      address: [...this.addresses],
      times: Array.from(validBackendTypes),
      title: searchTitle,
      categoryIds: [...this.categoryIds],
    };
  }

  // Event handlers from Search Bar
  onMainSearch(data: { keyword: string; categoryId: number | ''; address: string }): void {
    this.keyword = data.keyword;
    if (data.categoryId !== '') {
      this.categoryIds = [Number(data.categoryId)];
    } else {
      this.categoryIds = [];
    }

    if (data.address.length > 0) {
      this.addresses = [data.address];
    } else {
      this.addresses = [];
    }

    this.updateUrlQueryParams(true);
  }

  // Event handlers from Sidebar & Mobile Drawer
  onCategoryChange(event: { id: number; checked: boolean }): void {
    if (event.checked) {
      if (!this.categoryIds.includes(event.id)) {
        this.categoryIds = [...this.categoryIds, event.id];
      }
    } else {
      this.categoryIds = this.categoryIds.filter((id) => id !== event.id);
    }
    this.updateUrlQueryParams(true);
  }

  onAddressChange(event: { city: string; checked: boolean }): void {
    if (event.checked) {
      if (!this.addresses.includes(event.city)) {
        this.addresses = [...this.addresses, event.city];
      }
    } else {
      this.addresses = this.addresses.filter((c) => c !== event.city);
    }
    this.updateUrlQueryParams(true);
  }

  onSalaryChange(salaryId: string): void {
    this.salaryRange = salaryId;
    this.updateUrlQueryParams(true);
  }

  onExperienceChange(expId: string): void {
    this.experience = expId;
    this.updateUrlQueryParams(true);
  }

  onLevelChange(lvlId: string): void {
    this.level = lvlId;
    this.updateUrlQueryParams(true);
  }

  onEmploymentTypeChange(event: { type: string; checked: boolean }): void {
    if (event.checked) {
      if (!this.employmentTypes.includes(event.type)) {
        this.employmentTypes = [...this.employmentTypes, event.type];
      }
    } else {
      this.employmentTypes = this.employmentTypes.filter((t) => t !== event.type);
    }
    this.updateUrlQueryParams(true);
  }

  onWorkplaceChange(event: { workplace: string; checked: boolean }): void {
    if (event.checked) {
      if (!this.workplaces.includes(event.workplace)) {
        this.workplaces = [...this.workplaces, event.workplace];
      }
    } else {
      this.workplaces = this.workplaces.filter((w) => w !== event.workplace);
    }
    this.updateUrlQueryParams(true);
  }

  onSortChange(newSort: string): void {
    this.sort = newSort;
    this.updateUrlQueryParams(true);
  }

  // Active Chips Removal
  onRemoveChip(chip: ActiveFilterChip): void {
    switch (chip.type) {
      case 'keyword':
        this.keyword = '';
        break;
      case 'category':
        this.categoryIds = this.categoryIds.filter((id) => id !== chip.value);
        break;
      case 'address':
        this.addresses = this.addresses.filter((a) => a !== chip.value);
        break;
      case 'employmentType':
        this.employmentTypes = this.employmentTypes.filter((t) => t !== chip.value);
        break;
      case 'workplace':
        this.workplaces = this.workplaces.filter((w) => w !== chip.value);
        break;
      case 'salary':
        this.salaryRange = 'all';
        break;
      case 'experience':
        this.experience = 'all';
        break;
      case 'level':
        this.level = 'all';
        break;
    }
    this.updateUrlQueryParams(true);
  }

  onClearAllFilters(): void {
    this.keyword = '';
    this.categoryIds = [];
    this.addresses = [];
    this.employmentTypes = [];
    this.workplaces = [];
    this.salaryRange = 'all';
    this.experience = 'all';
    this.level = 'all';
    this.updateUrlQueryParams(true);
  }

  // Pagination Change
  onPageChange(pageNumber: number): void {
    this.pageIndex = pageNumber - 1;
    this.updateUrlQueryParams(false);
    this.scrollToResults();
  }

  // Mobile Drawer toggles
  openMobileDrawer(): void {
    this.isMobileDrawerOpen = true;
  }

  closeMobileDrawer(): void {
    this.isMobileDrawerOpen = false;
  }

  private scrollToResults(): void {
    if (!this.isBrowser) return;

    requestAnimationFrame(() => {
      const el = document.getElementById('job-results-container');
      if (el) {
        const offset = 90;
        const bodyRect = document.body.getBoundingClientRect().top;
        const elementRect = el.getBoundingClientRect().top;
        const elementPosition = elementRect - bodyRect;
        const offsetPosition = elementPosition - offset;

        window.scrollTo({
          top: Math.max(0, offsetPosition),
          behavior: 'smooth',
        });
      }
    });
  }
}
