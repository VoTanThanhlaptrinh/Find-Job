import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BlogService } from '../../services/blog.service';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';

@Component({
  selector: 'app-blog-creation',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  standalone: true,
  templateUrl: './blog-creation.component.html',
  styleUrl: './blog-creation.component.css'
})
export class BlogCreationComponent implements OnInit {
  isSubmitting = false;

  blogForm = new FormGroup({
    title: new FormControl('', [Validators.required, Validators.maxLength(255)]),
    description: new FormControl('', [Validators.required, Validators.maxLength(500)]),
    content: new FormControl('', [Validators.required])
  });

  constructor(
    private blogService: BlogService,
    private authService: AuthService,
    private tokenService: TokenService,
    private router: Router,
    private notify: NotifyMessageService
  ) {}

  ngOnInit(): void {
    // Simple check using existing AuthService & TokenService
    if (!this.tokenService.getToken() || !this.authService.isLoggedIn()) {
      this.notify.warning('Vui lòng đăng nhập để tạo bài viết.');
      this.router.navigate(['/'], { queryParams: { auth: 'login' } });
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.blogForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  countWords(text: string | null | undefined): number {
    if (!text) return 0;
    return text.trim().split(/\s+/).filter(w => w.length > 0).length;
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
