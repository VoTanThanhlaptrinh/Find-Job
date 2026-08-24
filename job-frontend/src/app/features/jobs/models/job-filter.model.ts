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
  count?: number;
  checked?: boolean;
}

export interface FilterGroupConfig<T = string | number> {
  id: string;
  title: string;
  isExpanded: boolean;
  options: FilterOption<T>[];
  showMore: boolean;
}

export const SALARY_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả mức lương' },
  { id: 'under-10', label: 'Dưới 10 triệu' },
  { id: '10-20', label: '10 - 20 triệu' },
  { id: '20-30', label: '20 - 30 triệu' },
  { id: '30-50', label: '30 - 50 triệu' },
  { id: 'above-50', label: 'Trên 50 triệu' },
  { id: 'deal', label: 'Thỏa thuận' },
];

export const EXPERIENCE_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả kinh nghiệm' },
  { id: 'none', label: 'Chưa có kinh nghiệm' },
  { id: 'under-1', label: 'Dưới 1 năm' },
  { id: '1-3', label: '1 - 3 năm' },
  { id: '3-5', label: '3 - 5 năm' },
  { id: 'above-5', label: 'Trên 5 năm' },
];

export const LEVEL_OPTIONS: FilterOption<string>[] = [
  { id: 'all', label: 'Tất cả cấp bậc' },
  { id: 'intern-fresher', label: 'Thực tập / Intern / Fresher' },
  { id: 'junior', label: 'Nhân viên / Junior' },
  { id: 'middle', label: 'Chuyên viên / Middle' },
  { id: 'senior', label: 'Chuyên gia / Senior' },
  { id: 'lead-manager', label: 'Trưởng nhóm / Quản lý' },
];

export const EMPLOYMENT_TYPE_OPTIONS: FilterOption<string>[] = [
  { id: 'FULL_TIME', label: 'Toàn thời gian' },
  { id: 'PART_TIME', label: 'Bán thời gian' },
  { id: 'INTERN', label: 'Thực tập sinh' },
  { id: 'CONTRACT', label: 'Hợp đồng / Dự án' },
];

export const WORKPLACE_OPTIONS: FilterOption<string>[] = [
  { id: 'ONSITE', label: 'Làm việc tại văn phòng (Onsite)' },
  { id: 'HYBRID', label: 'Linh hoạt (Hybrid)' },
  { id: 'REMOTE', label: 'Làm từ xa (Remote)' },
];

export const SORT_OPTIONS: { id: string; label: string }[] = [
  { id: 'relevant', label: 'Phù hợp nhất' },
  { id: 'newest', label: 'Mới nhất' },
  { id: 'salary-desc', label: 'Lương cao nhất' },
  { id: 'salary-asc', label: 'Lương thấp nhất' },
];
