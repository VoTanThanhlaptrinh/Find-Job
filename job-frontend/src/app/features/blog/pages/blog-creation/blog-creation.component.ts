import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BlogService } from '../../services/blog.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';
import { MarkdownEditorComponent } from '../../../../shared/components/markdown-editor/markdown-editor.component';
import { MarkdownViewerComponent } from '../../../../shared/components/markdown-viewer/markdown-viewer.component';

@Component({
  selector: 'app-blog-creation',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, MarkdownEditorComponent, MarkdownViewerComponent],
  standalone: true,
  templateUrl: './blog-creation.component.html',
  styleUrl: './blog-creation.component.css'
})
export class BlogCreationComponent implements OnInit {
  isSubmitting = false;
  activeTab: 'edit' | 'preview' = 'edit';

  blogForm = new FormGroup({
    title: new FormControl('Hướng dẫn viết bài Blog với Markdown chuẩn đẹp', [Validators.required, Validators.maxLength(255)]),
    description: new FormControl('Trình soạn thảo Markdown chuyên nghiệp hỗ trợ xem trước thời gian thực, bảng biểu và tô màu code.', [Validators.required, Validators.maxLength(500)]),
    content: new FormControl(`# Chào mừng bạn đến với Trình soạn thảo Markdown!

Đây là bài viết mẫu được soạn bằng **Markdown**. Trình soạn thảo hỗ trợ chia đôi màn hình (*Split View*) giúp bạn vừa gõ vừa theo dõi kết quả hiển thị thời gian thực.

## 1. Tính năng nổi bật
- **In đậm**, *in nghiêng*, ~~gạch ngang~~
- Hỗ trợ phím tắt: \`Ctrl+B\`, \`Ctrl+I\`, \`Ctrl+K\`
- Thụt lề thông minh với phím \`Tab\`
- Danh sách việc cần làm (Task checklist):
  - [x] Cài đặt thư viện \`ngx-markdown\` và \`prismjs\`
  - [x] Tạo component dùng chung \`MarkdownEditorComponent\`
  - [ ] Thử nghiệm viết bài thực tế

## 2. Khối mã nguồn (Code Block)
\`\`\`typescript
interface UserProfile {
  id: number;
  name: string;
  role: 'ADMIN' | 'RECRUITER' | 'CANDIDATE';
}

const greet = (user: UserProfile): string => {
  return \`Xin chào \${user.name}!\`;
};
\`\`\`

## 3. Bảng biểu mẫu (Markdown Table)
| Kỹ năng | Mức độ | Đánh giá |
| :--- | :---: | ---: |
| Angular 20 | Cao cấp | 9.5/10 |
| Tailwind CSS | Thành thạo | 9.0/10 |
| Markdown & SSR | Tốt | 8.8/10 |

> "Viết nội dung mạch lạc và cấu trúc rõ ràng với Markdown giúp người đọc dễ tiếp thu thông tin hơn."
`, [Validators.required, Validators.maxLength(10000)])
  });

  constructor(
    private blogService: BlogService,
    private authService: AuthService,
    private tokenService: TokenService,
    private router: Router,
    private notify: NotifyMessageService
  ) {}

  ngOnInit(): void {
    // Tạm thời mở truy cập để trải nghiệm và xem thử giao diện editor Markdown
    // if (!this.tokenService.getToken() || !this.authService.isLoggedIn()) {
    //   this.notify.warning('Vui lòng đăng nhập để tạo bài viết.');
    //   this.router.navigate(['/'], { queryParams: { auth: 'login' } });
    // }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.blogForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  countWords(text: string | null | undefined): number {
    if (!text) return 0;
    return text.trim().split(/\s+/).filter(w => w.length > 0).length;
  }

  setActiveTab(tab: 'edit' | 'preview'): void {
    this.activeTab = tab;
  }

  get estimatedReadingMinutes(): number {
    const words = this.countWords(this.blogForm.value.content);
    return Math.max(1, Math.ceil(words / 200));
  }

  onSubmit(): void {
    if (this.blogForm.invalid) {
      this.blogForm.markAllAsTouched();
      this.notify.warning('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }

    this.isSubmitting = true;
    const payload = {
      title: this.blogForm.value.title?.trim() || '',
      description: this.blogForm.value.description?.trim() || '',
      content: this.blogForm.value.content?.trim() || ''
    };

    this.blogService.postBlog(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.notify.success('Bài viết đã được tạo thành công!');
        this.router.navigate(['/blogHome']);
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error creating blog:', err);
        this.notify.error(err?.error?.message || 'Có lỗi xảy ra khi tạo bài viết.');
      }
    });
  }
}
