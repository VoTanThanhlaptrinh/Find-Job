import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { I18nService } from '../../i18n/i18n.service';

export interface LegalModalData {
  type: 'privacy' | 'terms';
}

@Component({
  selector: 'app-legal-modal',
  standalone: true,
  imports: [CommonModule, MatDialogModule],
  template: `
    <div class="relative max-w-2xl overflow-hidden rounded-2xl bg-white p-6 sm:p-8 font-body shadow-2xl">
      <!-- Close button -->
      <button
        type="button"
        (click)="close()"
        aria-label="Đóng"
        class="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-xl bg-slate-100 text-slate-500 transition hover:bg-slate-200 hover:text-slate-800 cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
      >
        <span class="material-symbols-outlined text-[20px]">close</span>
      </button>

      <!-- Header -->
      <div class="mb-5 flex items-center gap-3 border-b border-slate-100 pb-4">
        <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-[#1E63F3]">
          <span class="material-symbols-outlined text-[24px]">{{ isPrivacy ? 'security' : 'gavel' }}</span>
        </div>
        <div>
          <h2 class="font-headline text-xl font-bold text-slate-900">
            {{ isPrivacy ? (isVi ? 'Chính sách bảo mật' : 'Privacy Policy') : (isVi ? 'Điều khoản sử dụng' : 'Terms of Service') }}
          </h2>
          <p class="text-xs text-slate-500">
            Job Listing Platform &bull; {{ isVi ? 'Cập nhật năm 2026' : 'Updated 2026' }}
          </p>
        </div>
      </div>

      <!-- Content -->
      <div class="max-h-[60vh] space-y-4 overflow-y-auto pr-2 text-sm leading-relaxed text-slate-600">
        @if (isPrivacy) {
          @if (isVi) {
            <p>
              Chào mừng bạn đến với nền tảng tuyển dụng <strong>Job Listing</strong>. Chúng tôi cam kết bảo vệ tuyệt đối thông tin cá nhân và hồ sơ ứng tuyển (CV) của bạn theo quy định pháp luật Việt Nam.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">1. Thu thập dữ liệu</h3>
            <p>
              Chúng tôi chỉ thu thập các thông tin cần thiết phục vụ quá trình tìm việc và kết nối nhà tuyển dụng, bao gồm: Họ tên, Email, Số điện thoại, Kinh nghiệm và File CV được bạn chủ động tải lên.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">2. Mục đích sử dụng</h3>
            <p>
              Dữ liệu của bạn được sử dụng nhằm mục đích gợi ý việc làm phù hợp, chuyển tiếp hồ sơ đến các nhà tuyển dụng uy tín mà bạn ứng tuyển và gửi thông báo trạng thái ứng tuyển.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">3. Cam kết bảo mật</h3>
            <p>
              Chúng tôi không chia sẻ hoặc bán dữ liệu người dùng cho bất kỳ bên thứ ba nào khi chưa có sự đồng thuận từ bạn.
            </p>
          } @else {
            <p>
              Welcome to <strong>Job Listing</strong> platform. We are committed to safeguarding your personal data and resume in full compliance with data privacy regulations.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">1. Data Collection</h3>
            <p>
              We collect essential information to facilitate your job search and recruitment matching: Full Name, Email, Phone Number, Professional Experience, and uploaded CV files.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">2. Data Usage</h3>
            <p>
              Your data is solely used to deliver tailored job recommendations, forward applications to employers you choose, and provide job status updates.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">3. Confidentiality</h3>
            <p>
              We do not share or sell your data to third parties without your explicit authorization.
            </p>
          }
        } @else {
          @if (isVi) {
            <p>
              Quy chế và điều khoản sử dụng này quy định các quyền và nghĩa vụ của người tìm việc và nhà tuyển dụng khi tham gia hệ sinh thái <strong>Job Listing</strong>.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">1. Trách nhiệm người dùng</h3>
            <p>
              Ứng viên cam kết cung cấp thông tin trung thực trong CV và hồ sơ cá nhân. Doanh nghiệp cam kết đăng tin tuyển dụng chính xác, minh bạch về mức lương và chế độ đãi ngộ.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">2. Quyền sở hữu trí tuệ</h3>
            <p>
              Mọi nội dung, giao diện, mã nguồn và biểu tượng trên nền tảng thuộc bản quyền của Job Listing và Thành Corporation.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">3. Hỗ trợ và giải quyết khiếu nại</h3>
            <p>
              Mọi vấn đề phát sinh sẽ được đội ngũ hỗ trợ tiếp nhận và xử lý nhanh chóng qua hotline hoặc email tư vấn chính thức.
            </p>
          } @else {
            <p>
              These Terms of Service govern the rights and responsibilities of job seekers and employers using the <strong>Job Listing</strong> platform.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">1. User Responsibilities</h3>
            <p>
              Job seekers agree to provide accurate qualifications in their resumes. Employers guarantee fair, transparent job postings with verifiable compensation.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">2. Intellectual Property</h3>
            <p>
              All platform designs, assets, and branding are the property of Job Listing and Thành Corporation.
            </p>
            <h3 class="font-bold text-slate-900 pt-2">3. Support</h3>
            <p>
              Inquiries and disputes are addressed promptly via our official support channels.
            </p>
          }
        }
      </div>

      <!-- Action Footer -->
      <div class="mt-6 flex justify-end border-t border-slate-100 pt-4">
        <button
          type="button"
          (click)="close()"
          class="rounded-xl bg-[#1E63F3] px-5 py-2.5 text-sm font-semibold text-white shadow-md shadow-[#1E63F3]/25 transition hover:bg-[#174FCB] cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-[#1E63F3]"
        >
          {{ isVi ? 'Đã hiểu' : 'Got it' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
  `]
})
export class LegalModalComponent {
  readonly dialogRef = inject(MatDialogRef<LegalModalComponent>);
  readonly data: LegalModalData = inject(MAT_DIALOG_DATA);
  private readonly i18nService = inject(I18nService);

  get isPrivacy(): boolean {
    return this.data?.type === 'privacy';
  }

  get isVi(): boolean {
    return this.i18nService.currentLanguage === 'vi';
  }

  close(): void {
    this.dialogRef.close();
  }
}
