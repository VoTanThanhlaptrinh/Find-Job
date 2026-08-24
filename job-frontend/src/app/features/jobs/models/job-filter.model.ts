export interface JobFilterState {
  keyword: string;
  categoryIds: number[];
  addresses: string[];
  employmentTypes: string[];
  salaryRange: string;
  experience: string;
  level: string;
  workplaces: string[];
  sort: string;
  page: number;
  pageSize: number;
}

export interface FilterOption<T = string | number> {
  id: T;
  label: string;
  labelKey?: string;
  count?: number;
  checked?: boolean;
}

export interface FilterGroupConfig<T = string | number> {
  id: string;
  title: string;
  titleKey?: string;
  isExpanded: boolean;
  options: FilterOption<T>[];
  showMore: boolean;
}

export const SALARY_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả mức lương', labelKey: 'category.filters.salary.all' },
  { id: 'under-10', label: 'Dưới 10 triệu', labelKey: 'category.filters.salary.under10' },
  { id: '10-20', label: '10 - 20 triệu', labelKey: 'category.filters.salary.range10_20' },
  { id: '20-30', label: '20 - 30 triệu', labelKey: 'category.filters.salary.range20_30' },
  { id: '30-50', label: '30 - 50 triệu', labelKey: 'category.filters.salary.range30_50' },
  { id: 'above-50', label: 'Trên 50 triệu', labelKey: 'category.filters.salary.above50' },
  { id: 'deal', label: 'Thỏa thuận', labelKey: 'category.filters.salary.deal' },
];

export const EXPERIENCE_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả kinh nghiệm', labelKey: 'category.filters.experience.all' },
  { id: 'none', label: 'Chưa có kinh nghiệm', labelKey: 'category.filters.experience.none' },
  { id: 'under-1', label: 'Dưới 1 năm', labelKey: 'category.filters.experience.under1' },
  { id: '1-3', label: '1 - 3 năm', labelKey: 'category.filters.experience.range1_3' },
  { id: '3-5', label: '3 - 5 năm', labelKey: 'category.filters.experience.range3_5' },
  { id: 'above-5', label: 'Trên 5 năm', labelKey: 'category.filters.experience.above5' },
];

export const LEVEL_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả cấp bậc', labelKey: 'category.filters.level.all' },
  { id: 'intern-fresher', label: 'Thực tập / Intern / Fresher', labelKey: 'category.filters.level.internFresher' },
  { id: 'junior', label: 'Nhân viên / Junior', labelKey: 'category.filters.level.junior' },
  { id: 'middle', label: 'Chuyên viên / Middle', labelKey: 'category.filters.level.middle' },
  { id: 'senior', label: 'Chuyên gia / Senior', labelKey: 'category.filters.level.senior' },
  { id: 'lead-manager', label: 'Trưởng nhóm / Quản lý', labelKey: 'category.filters.level.leadManager' },
];

export const EMPLOYMENT_TYPE_OPTIONS: FilterOption<string>[] = [
  { id: 'FULL_TIME', label: 'Toàn thời gian', labelKey: 'category.filters.employmentType.fullTime' },
  { id: 'PART_TIME', label: 'Bán thời gian', labelKey: 'category.filters.employmentType.partTime' },
  { id: 'INTERN', label: 'Thực tập sinh', labelKey: 'category.filters.employmentType.intern' },
  { id: 'CONTRACT', label: 'Hợp đồng / Dự án', labelKey: 'category.filters.employmentType.contract' },
];

export const WORKPLACE_OPTIONS: FilterOption<string>[] = [
  { id: 'ONSITE', label: 'Làm việc tại văn phòng (Onsite)', labelKey: 'category.filters.workplace.onsite' },
  { id: 'HYBRID', label: 'Linh hoạt (Hybrid)', labelKey: 'category.filters.workplace.hybrid' },
  { id: 'REMOTE', label: 'Làm từ xa (Remote)', labelKey: 'category.filters.workplace.remote' },
];

export const SORT_OPTIONS: { id: string; label: string; labelKey: string }[] = [
  { id: 'relevant', label: 'Phù hợp nhất', labelKey: 'category.results.sortOptions.relevant' },
  { id: 'newest', label: 'Mới nhất', labelKey: 'category.results.sortOptions.newest' },
  { id: 'salary-desc', label: 'Lương cao nhất', labelKey: 'category.results.sortOptions.salaryDesc' },
  { id: 'salary-asc', label: 'Lương thấp nhất', labelKey: 'category.results.sortOptions.salaryAsc' },
];
