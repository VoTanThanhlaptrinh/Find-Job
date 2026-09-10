import { Component, effect, NO_ERRORS_SCHEMA, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { JobDetailViewModel, AddressCountViewModel } from '../../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { CareerBlogItem } from '../../../../shared/models/jobs/career-blog.model';
import { JobService } from '../../services/job.service';
import { SafeHtmlPipe } from '../../../../shared/pipes/safe-html.pipe';
import { AuthService } from '../../../../core/services/auth.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { SkeletonJobSingleComponent } from '../../../../shared/components/skeleton-job-single/skeleton-job-single.component';
import { FilterService } from '../../services/filter.service';
import { BlogService } from '../../../blog/services/blog.service';

@Component({
  selector: 'app-job-single',
  imports: [CommonModule, RouterModule, SafeHtmlPipe, TranslatePipe, SkeletonJobSingleComponent],
  standalone: true,
  templateUrl: './job-single.component.html',
  styleUrl: './job-single.component.css',
  schemas: [NO_ERRORS_SCHEMA],
})
export class JobSingleComponent implements OnInit {
  jobId!: string;
  private readonly currentJobId = signal<string>('');

  jobDetail: JobDetailViewModel = {
    id: '',
    title: '',
    address: '',
    description: '',
    salary: '',
    time: '',
    requireDetails: '',
    skill: '',
    expiredDate: '',
    headcount: 0,
    companyName: '',
    companyLogo: '',
    companyDescription: '',
    companyWebsite: '',
    categoryName: '',
    experienceYears: undefined,
    locationCity: '',
  };

  relatedJobs: JobCardModel[] = [];
  hasApplied: boolean | null = null;
  isCheckingApply = false;
  isLoading = false;
  addressCounts: AddressCountViewModel[] = [];

  // Career Advice Blogs
  careerBlogs: CareerBlogItem[] = [];
  isLoadingBlogs = false;

  // Company intro expand/collapse state
  isCompanyExpanded = false;

  constructor(
    private jobService: JobService,
    private route: ActivatedRoute,
    private authService: AuthService,
    private i18nService: I18nService,
    private filterService: FilterService,
    private blogService: BlogService,
  ) {
    this.jobService.resetJobDetailState();
    this.jobService.resetCheckApplyState();

    effect(() => {
      this.jobDetail = this.jobService.jobDetail$() ?? {
        id: '',
        title: '',
        address: '',
        description: '',
        salary: '',
        time: '',
        requireDetails: '',
        skill: '',
        expiredDate: '',
        headcount: 0,
        companyName: '',
        companyLogo: '',
        companyDescription: '',
        companyWebsite: '',
        categoryName: '',
        experienceYears: undefined,
        locationCity: '',
      };
      this.hasApplied = this.jobService.hasApplied$();
      this.isCheckingApply = this.jobService.isCheckingApply$();
      this.isLoading = this.jobService.isLoadingJobDetail$();
      this.addressCounts = this.filterService.addressCount();
    });

    effect(() => {
      const jobId = this.currentJobId();
      const isAuthReady = this.authService.isAuthReady();
      const isLoggedIn = this.authService.isLoggedIn();

      if (!jobId || !isAuthReady) {
        return;
      }

      const parsedJobId = Number(jobId);
      if (!isLoggedIn || Number.isNaN(parsedJobId) || parsedJobId <= 0) {
        this.jobService.resetCheckApplyState();
        return;
      }

      this.jobService.checkApplyJob(parsedJobId);
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.jobId = params['id'];
      this.currentJobId.set(this.jobId);
      this.getDetailJob(this.jobId);
    });
    this.filterService.getAddressCount();
    this.loadCareerBlogs();
  }

  getDetailJob(id: string): void {
    this.jobService.getDetailJob(id);
  }

  checkApplyStatus(): void {
    const parsedJobId = Number(this.jobId);

    if (!this.authService.isLoggedIn() || Number.isNaN(parsedJobId) || parsedJobId <= 0) {
      this.jobService.resetCheckApplyState();
      this.hasApplied = null;
      return;
    }

    this.jobService.checkApplyJob(parsedJobId);
  }

  get showApplyButton(): boolean {
    if (!this.authService.isAuthReady()) {
      return false;
    }

    if (!this.authService.isLoggedIn()) {
      return true;
    }

    return !this.isCheckingApply && this.hasApplied === false;
  }

  get showApplyDisabledState(): boolean {
    if (!this.authService.isAuthReady()) {
      return true;
    }

    return this.authService.isLoggedIn() && this.isCheckingApply;
  }

  toggleCompanyExpand(): void {
    this.isCompanyExpanded = !this.isCompanyExpanded;
  }

  loadCareerBlogs(): void {
    this.isLoadingBlogs = true;
    this.blogService.blogList(0, 3).subscribe({
      next: (res) => {
        const rawContent = res?.data?.content || [];
        if (Array.isArray(rawContent) && rawContent.length > 0) {
          this.careerBlogs = rawContent.slice(0, 3).map((item: any, idx: number) => ({
            id: item.id || idx + 1,
            title: item.title || 'Kinh nghiệm nghề nghiệp hữu ích',
            category: item.categoryName || this.getDefaultCategory(idx),
            description: item.description || '',
            image: item.image || this.getDefaultBlogImage(idx),
            date: this.formatBlogDate(item.create_date || item.createdAt),
            readTime: '5 phút đọc',
          }));
        } else {
          this.careerBlogs = this.getFallbackCareerBlogs();
        }
        this.isLoadingBlogs = false;
      },
      error: () => {
        this.careerBlogs = this.getFallbackCareerBlogs();
        this.isLoadingBlogs = false;
      },
    });
  }

  private getDefaultCategory(index: number): string {
    const categories = ['Bí quyết viết CV', 'Kỹ năng phỏng vấn', 'Định hướng nghề nghiệp'];
    return categories[index % categories.length];
  }

  private getDefaultBlogImage(index: number): string {
    const images = [
      'assets/web_css/img/blog1.jpg',
      'assets/web_css/img/blog2.jpg',
      'assets/web_css/img/b1.jpg',
    ];
    return images[index % images.length];
  }

  private formatBlogDate(dateStr?: string): string {
    if (!dateStr) return 'Gần đây';
    try {
      const d = new Date(dateStr);
      if (Number.isNaN(d.getTime())) return dateStr;
      return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
    } catch {
      return 'Gần đây';
    }
  }

  private getFallbackCareerBlogs(): CareerBlogItem[] {
    return [
      {
        id: 1,
        title: 'Bí quyết tối ưu CV công nghệ thu hút nhà tuyển dụng ngay trong 30 giây đầu',
        category: 'Bí quyết viết CV',
        description: 'Khám phá các phương pháp chọn lọc từ khóa chuyên môn, cấu trúc kết quả định lượng và trình bày dự án nổi bật.',
        image: 'assets/web_css/img/blog1.jpg',
        date: '10/09/2026',
        readTime: '5 phút đọc',
      },
      {
        id: 2,
        title: 'Chiến lược vượt qua vòng phỏng vấn kỹ thuật và đàm phán mức lương kỳ vọng',
        category: 'Kỹ năng phỏng vấn',
        description: 'Cách làm chủ các bài kiểm tra chuyên sâu, thể hiện tư duy giải quyết vấn đề và định vị giá trị của bản thân.',
        image: 'assets/web_css/img/blog2.jpg',
        date: '08/09/2026',
        readTime: '7 phút đọc',
      },
      {
        id: 3,
        title: 'Lộ trình nâng cao năng lực và xây dựng sự nghiệp bền vững trong kỷ nguyên AI',
        category: 'Định hướng nghề nghiệp',
        description: 'Những kỹ năng bổ trợ không thể thiếu giúp kỹ sư phần mềm thích ứng nhanh với các chuyển biến công nghệ mới.',
        image: 'assets/web_css/img/b1.jpg',
        date: '05/09/2026',
        readTime: '6 phút đọc',
      },
    ];
  }

  /**
   * Bóc tách các kỹ năng từ HTML string hoặc text phân cách bởi dấu phẩy/gạch đầu dòng
   */
  get skillTags(): string[] {
    const raw = this.jobDetail.skill;
    if (!raw || typeof raw !== 'string') {
      return [];
    }

    // Nếu chứa <li>
    if (raw.includes('<li')) {
      const regex = /<li[^>]*>([\s\S]*?)<\/li>/gi;
      const tags: string[] = [];
      let match: RegExpExecArray | null;
      while ((match = regex.exec(raw)) !== null) {
        const text = match[1].replace(/<[^>]*>/g, '').trim();
        if (text) {
          tags.push(text);
        }
      }
      if (tags.length > 0) {
        return tags;
      }
    }

    // Nếu chứa dấu phẩy
    if (raw.includes(',')) {
      return raw
        .replace(/<[^>]*>/g, '')
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    }

    // Nếu dạng gạch đầu dòng xuống hàng
    if (raw.includes('\n')) {
      return raw
        .replace(/<[^>]*>/g, '')
        .split('\n')
        .map((s) => s.replace(/^[-*•\s]+/, '').trim())
        .filter(Boolean);
    }

    // Fallback: 1 tag nếu có text
    const clean = raw.replace(/<[^>]*>/g, '').trim();
    return clean ? [clean] : [];
  }

  /**
   * Kiểm tra xem phần skill có nội dung rich text / diễn giải chi tiết ngoài danh sách tags không
   */
  get skillHasRichExplanation(): boolean {
    const raw = this.jobDetail.skill;
    if (!raw) return false;
    return raw.includes('<p') || raw.length > 250;
  }

  /**
   * Định dạng mức lương thân thiện
   */
  formatSalary(salary: string | number | undefined): string {
    if (salary === null || salary === undefined || salary === '' || salary === 0) {
      return this.i18nService.currentLanguage === 'vi' ? 'Thỏa thuận' : 'Negotiable';
    }

    if (typeof salary === 'string') {
      const trimmed = salary.trim();
      if (trimmed.length > 0 && trimmed !== '0') {
        return trimmed;
      }
      return this.i18nService.currentLanguage === 'vi' ? 'Thỏa thuận' : 'Negotiable';
    }

    const locale = this.i18nService.currentLanguage === 'vi' ? 'vi-VN' : 'en-US';
    return `${salary.toLocaleString(locale)} VNĐ`;
  }

  /**
   * Chuyển đổi Employment Type sang nhãn hiển thị trực quan
   */
  getEmploymentTypeLabel(time?: string): string {
    if (!time) {
      return this.i18nService.currentLanguage === 'vi' ? 'Toàn thời gian' : 'Full-time';
    }

    const upper = time.toUpperCase();
    if (upper.includes('REMOTE')) {
      return this.i18nService.currentLanguage === 'vi' ? 'Làm việc từ xa (Remote)' : 'Remote';
    }
    if (upper.includes('PART') || upper.includes('PART_TIME')) {
      return this.i18nService.currentLanguage === 'vi' ? 'Bán thời gian' : 'Part-time';
    }
    return this.i18nService.currentLanguage === 'vi' ? 'Toàn thời gian' : 'Full-time';
  }

  /**
   * Định dạng ngày hết hạn
   */
  formatDate(dateStr?: string): string {
    if (!dateStr) {
      return this.i18nService.currentLanguage === 'vi' ? 'Đang cập nhật' : 'Updating';
    }

    try {
      const d = new Date(dateStr);
      if (Number.isNaN(d.getTime())) return dateStr;
      const day = d.getDate().toString().padStart(2, '0');
      const month = (d.getMonth() + 1).toString().padStart(2, '0');
      const year = d.getFullYear();
      return `${day}/${month}/${year}`;
    } catch {
      return dateStr;
    }
  }

  /**
   * Tính phần trăm số lượng tin việc làm cho thanh ngang mini
   */
  get maxAddressCount(): number {
    if (!this.addressCounts || this.addressCounts.length === 0) return 1;
    return Math.max(...this.addressCounts.map((a) => a.count || 0), 1);
  }

  getAddressPercent(count: number): number {
    const percent = Math.round(((count || 0) / this.maxAddressCount) * 100);
    return Math.max(8, Math.min(100, percent));
  }

  /**
   * Top 5 khu vực tuyển dụng nhiều nhất
   */
  get topAddressCounts(): AddressCountViewModel[] {
    if (!this.addressCounts) return [];
    return [...this.addressCounts]
      .sort((a, b) => (b.count || 0) - (a.count || 0))
      .slice(0, 5);
  }

  /**
   * Giá trị hiển thị an toàn cho Tên công ty
   */
  get companyDisplayName(): string {
    return (this.jobDetail.companyName || '').trim() || 'Lumina Systems Inc.';
  }

  /**
   * Giới thiệu ngắn về công ty
   */
  get companyDescriptionText(): string {
    if (this.jobDetail.companyDescription && this.jobDetail.companyDescription.trim()) {
      return this.jobDetail.companyDescription.trim();
    }
    return 'Lumina Systems Inc. là công ty công nghệ tiên phong phát triển các giải pháp phần mềm và hệ thống thông minh, cung cấp môi trường làm việc năng động, chuyên nghiệp và cơ hội phát triển vượt bậc cho nhân sự.';
  }

  /**
   * Logo công ty hiển thị
   */
  get companyLogoUrl(): string {
    if (this.jobDetail.companyLogo && this.jobDetail.companyLogo.trim()) {
      return this.jobDetail.companyLogo.trim();
    }
    return 'assets/web_css/img/post.png';
  }

  /**
   * Kinh nghiệm hiển thị
   */
  get experienceDisplay(): string {
    if (this.jobDetail.experienceYears !== undefined && this.jobDetail.experienceYears !== null) {
      if (this.jobDetail.experienceYears === 0) {
        return this.i18nService.currentLanguage === 'vi' ? 'Không yêu cầu kinh nghiệm' : 'No experience required';
      }
      return `${this.jobDetail.experienceYears} ${this.i18nService.currentLanguage === 'vi' ? 'năm kinh nghiệm' : 'years experience'}`;
    }
    return this.i18nService.currentLanguage === 'vi' ? '1 - 3 năm kinh nghiệm' : '1 - 3 years experience';
  }

  /**
   * Cấp bậc hiển thị
   */
  get levelDisplay(): string {
    const title = (this.jobDetail.title || '').toLowerCase();
    if (title.includes('senior') || title.includes('trưởng') || title.includes('lead')) {
      return this.i18nService.currentLanguage === 'vi' ? 'Chuyên viên cao cấp / Senior' : 'Senior Level';
    }
    if (title.includes('junior') || title.includes('fresher') || title.includes('intern')) {
      return this.i18nService.currentLanguage === 'vi' ? 'Mới tốt nghiệp / Junior' : 'Junior / Entry Level';
    }
    if (title.includes('manager') || title.includes('quản lý')) {
      return this.i18nService.currentLanguage === 'vi' ? 'Quản lý / Manager' : 'Manager';
    }
    return this.i18nService.currentLanguage === 'vi' ? 'Chuyên viên' : 'Specialist';
  }
}
