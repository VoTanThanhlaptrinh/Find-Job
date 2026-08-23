import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
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
export class HomeComponent {
  tags = ['Công nghệ', 'Kinh doanh', 'Tư vấn', 'Thiết kế', 'Lập trình'];

  // Testimonials
  testimonials: TestimonialItem[] = [
    {
      name: 'Trần Minh Đức',
      role: 'Senior Frontend Engineer',
      company: 'VNG Corporation',
      avatar: 'assets/web_css/img/r1.png',
      rating: 5,
      content: 'Nền tảng giúp tôi tìm được vị trí Senior ưng ý chỉ sau 1 tuần. Tính năng gợi ý việc làm rất sát với định hướng công nghệ và kỳ vọng đãi ngộ của tôi.',
      tag: 'Ứng viên tiêu biểu',
      tagClass: 'bg-blue-100 text-[#1E63F3]'
    },
    {
      name: 'Nguyễn Thu Trang',
      role: 'Head of Talent Acquisition',
      company: 'FPT Software',
      avatar: 'assets/web_css/img/r2.png',
      rating: 5,
      content: 'Từ khi áp dụng hệ thống tuyển dụng này, thời gian tìm kiếm ứng viên chất lượng của chúng tôi giảm hơn 40%. Tỷ lệ phản hồi từ ứng viên rất nhanh và chuyên nghiệp.',
      tag: 'Nhà tuyển dụng',
      tagClass: 'bg-emerald-100 text-emerald-700'
    },
    {
      name: 'Lê Hoàng Nam',
      role: 'Product Design Lead',
      company: 'Techcombank',
      avatar: 'assets/web_css/img/user.jpg',
      rating: 5,
      content: 'Trải nghiệm tạo CV và ứng tuyển cực kỳ mượt mà. Thông tin lương thưởng minh bạch giúp tôi tự tin đàm phán đãi ngộ tốt hơn.',
      tag: 'Ứng viên tiêu biểu',
      tagClass: 'bg-blue-100 text-[#1E63F3]'
    }
  ];

  // FAQ Items
  faqItems: FaqItem[] = [
    {
      question: 'Tôi có phải trả phí khi tìm việc hoặc tạo CV trên nền tảng không?',
      answer: 'Hoàn toàn không. Nền tảng miễn phí 100% cho mọi ứng viên khi tạo CV, tìm việc, ứng tuyển và kết nối với các doanh nghiệp.'
    },
    {
      question: 'Hệ thống gợi ý việc làm hoạt động như thế nào?',
      answer: 'Công nghệ AI phân tích các kỹ năng, kinh nghiệm và mong muốn nghề nghiệp trong hồ sơ của bạn để tự động đề xuất những vị trí tuyển dụng có độ tương thích cao nhất.'
    },
    {
      question: 'Nhà tuyển dụng sẽ liên hệ với tôi qua hình thức nào?',
      answer: 'Nhà tuyển dụng sẽ liên hệ trực tiếp qua số điện thoại, email hoặc gửi thông báo mời phỏng vấn thông qua hệ thống quản lý ứng tuyển của website.'
    },
    {
      question: 'Doanh nghiệp muốn đăng tin tuyển dụng thì bắt đầu như thế nào?',
      answer: 'Bạn chỉ cần truy cập vào cổng "Dành cho Doanh nghiệp" ở thanh điều hướng, đăng ký tài khoản nhà tuyển dụng và bắt đầu tạo tin tuyển dụng trong vòng chưa đầy 2 phút.'
    },
    {
      question: 'Thông tin cá nhân của tôi có được bảo mật không?',
      answer: 'Chúng tôi cam kết bảo mật tuyệt đối dữ liệu cá nhân theo tiêu chuẩn an ninh cao nhất. Bạn hoàn toàn có quyền ẩn hồ sơ hoặc chỉ cho phép các doanh nghiệp được chọn xem thông tin.'
    }
  ];

  expandedFaqIndex: number | null = 0;

  contactData: ContactFormModel = {
    name: '',
    email: '',
    subject: '',
    message: ''
  };
  contactSubmitted = false;

  constructor(private router: Router) {}

  toggleFaq(index: number): void {
    this.expandedFaqIndex = this.expandedFaqIndex === index ? null : index;
  }

  searchByTag(tag: string): void {
    this.router.navigate(['/category'], { queryParams: { keyword: tag } });
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
