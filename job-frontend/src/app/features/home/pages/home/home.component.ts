import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Component, inject, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { DownloadAreaComponent } from '../../../../shared/components/download-area/download-area.component';
import { SearchFormComponent } from '../../../../shared/components/search-form/search-form.component';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { FeaturesSectionComponent } from '../../components/features-section/features-section.component';
import { CategorySectionComponent } from '../../components/category-section/category-section.component';
import { AboutSectionComponent } from '../../components/about-section/about-section.component';
import { ContactFormModel, FaqItem, TestimonialItem } from '../../models/home.model';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { TRANSLATIONS } from '../../../../core/i18n/translations';

@Component({
  selector: 'app-home',
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    SearchFormComponent,
    CallToActionComponent,
    DownloadAreaComponent,
    FeaturesSectionComponent,
    CategorySectionComponent,
    AboutSectionComponent,
    TranslatePipe,
  ],
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit, OnDestroy {
  private readonly i18nService = inject(I18nService);
  private readonly router = inject(Router);

  currentHeadlineIndex = 0;
  currentEyebrowIndex = 0;
  isFlipping = false;
  private rotationTimer?: any;

  expandedFaqIndex: number | null = 0;

  contactData: ContactFormModel = {
    name: '',
    email: '',
    subject: '',
    message: '',
  };
  contactSubmitted = false;

  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  private get currentTranslations(): any {
    const lang = this.i18nService.currentLanguage;
    return TRANSLATIONS[lang]?.['home'] || TRANSLATIONS['en']['home'];
  }

  get headlinePhrases(): string[] {
    return (
      this.currentTranslations?.hero?.headlinePhrases || [
        'Tiến xa hơn.',
        'Bứt phá thu nhập.',
        'Nâng tầm sự nghiệp.',
        'Chinh phục ước mơ.',
        'Phát triển tương lai.',
      ]
    );
  }

  get eyebrowPhrases(): string[] {
    return (
      this.currentTranslations?.hero?.eyebrows || [
        '1.500+ CƠ HỘI MỚI MỖI NGÀY',
        '5.000+ DOANH NGHIỆP HÀNG ĐẦU',
        '100.000+ ỨNG VIÊN TIN DÙNG',
        'KẾT NỐI NHANH TRONG 24H',
      ]
    );
  }

  get tags(): string[] {
    return (
      this.currentTranslations?.hero?.tags || [
        'Công nghệ',
        'Kinh doanh',
        'Tư vấn',
        'Thiết kế',
        'Lập trình',
      ]
    );
  }

  get testimonials(): TestimonialItem[] {
    const reviews = this.currentTranslations?.testimonials?.reviews || [];
    const avatars = [
      'assets/web_css/img/r1.png',
      'assets/web_css/img/r2.png',
      'assets/web_css/img/user.jpg',
    ];
    const tagClasses = [
      'bg-blue-100 text-[#1E63F3]',
      'bg-emerald-100 text-emerald-700',
      'bg-blue-100 text-[#1E63F3]',
    ];

    return reviews.map((r: any, idx: number) => ({
      name: r.name,
      role: r.role,
      company: r.company,
      avatar: avatars[idx % avatars.length],
      rating: 5,
      content: r.content,
      tag: r.tag,
      tagClass: tagClasses[idx % tagClasses.length],
    }));
  }

  get faqItems(): FaqItem[] {
    return this.currentTranslations?.faq?.items || [];
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.rotationTimer = setInterval(() => {
        this.isFlipping = true;
        setTimeout(() => {
          const headlines = this.headlinePhrases;
          const eyebrows = this.eyebrowPhrases;
          this.currentHeadlineIndex = (this.currentHeadlineIndex + 1) % (headlines.length || 1);
          this.currentEyebrowIndex = (this.currentEyebrowIndex + 1) % (eyebrows.length || 1);
          this.isFlipping = false;
        }, 300);
      }, 2000);
    }
  }

  ngOnDestroy(): void {
    if (this.rotationTimer) {
      clearInterval(this.rotationTimer);
    }
  }

  toggleFaq(index: number): void {
    this.expandedFaqIndex = this.expandedFaqIndex === index ? null : index;
  }

  searchByTag(tag: string): void {
    this.router.navigate(['/jobs'], { queryParams: { keyword: tag } });
  }

  onContactSubmit(): void {
    if (this.contactData.name && this.contactData.email && this.contactData.message) {
      this.contactSubmitted = true;
    }
  }

  resetContact(): void {
    this.contactSubmitted = false;
    this.contactData = { name: '', email: '', subject: '', message: '' };
  }
}
