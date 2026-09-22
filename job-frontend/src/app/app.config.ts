import { isPlatformBrowser } from '@angular/common';
import { ApplicationConfig, importProvidersFrom, inject, PLATFORM_ID, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { loggerInterceptor } from './core/interceptors/logger.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { refreshTokenInterceptor } from './core/interceptors/refresh-token.interceptor';
import { provideQuillConfig } from 'ngx-quill';
import hljs from 'highlight.js';
import { provideNativeDateAdapter } from '@angular/material/core';
import { JwtModule } from '@auth0/angular-jwt';
import { provideToastr } from 'ngx-toastr';
import { AuthService } from './core/services/auth.service';
import { provideMarkdown, CLIPBOARD_OPTIONS, SANITIZE } from 'ngx-markdown';
import DOMPurify from 'dompurify';
import ClipboardJS from 'clipboard';
import { MarkdownClipboardButtonComponent } from './shared/components/markdown-clipboard-button/markdown-clipboard-button.component';

// Đảm bảo biến ClipboardJS luôn tồn tại trên window khi chạy trên trình duyệt
if (typeof window !== 'undefined') {
  (window as any).ClipboardJS = ClipboardJS;
}

// Import Prism languages for syntax highlighting
import 'prismjs';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-markdown';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-scss';
import 'prismjs/components/prism-sql';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-python';
import 'prismjs/components/prism-csharp';

export function sanitizeHtml(html: string): string {
  if (typeof window !== 'undefined') {
    const purify = (DOMPurify as any).default || DOMPurify;
    return typeof purify.sanitize === 'function' ? purify.sanitize(html) : purify(window).sanitize(html);
  }
  return html;
}

export function initializeApp(authService: AuthService) {
  return () => authService.refreshToken();
}
export const appConfig: ApplicationConfig = {
  providers: [
    provideMarkdown({
      clipboardOptions: {
        provide: CLIPBOARD_OPTIONS,
        useValue: {
          buttonComponent: MarkdownClipboardButtonComponent,
        },
      },
      sanitize: {
        provide: SANITIZE,
        useValue: sanitizeHtml,
      },
    }),
    provideZoneChangeDetection({ eventCoalescing: true }),

    provideRouter(
      routes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'enabled',
        anchorScrolling: 'enabled'
      })
    ),
    provideHttpClient(
      withFetch(),
      withInterceptors([loggerInterceptor, errorInterceptor, refreshTokenInterceptor])
    ),
    importProvidersFrom(JwtModule.forRoot({})),
    provideNativeDateAdapter(),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      newestOnTop: true,
      maxOpened: 4,
    }),
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      const platformId = inject(PLATFORM_ID);

      if (isPlatformBrowser(platformId)) {
        authService.refreshToken().subscribe();
      } else {
        authService.markAuthReady();
      }
    }),
    provideAnimations(),
    provideQuillConfig({
      modules: {
        syntax: { hljs },
        toolbar: [
          ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
          ['blockquote', 'code-block'],                     // quote & code block
          ['link', 'image', 'video', 'formula'],            // link, chèn ảnh, video, công thức
          [{ header: 1 }, { header: 2 }],                   // tiêu đề cấp 1 & 2
          [{ list: 'ordered' }, { list: 'bullet' }, { list: 'check' }],  // danh sách liệt kê
          [{ script: 'sub' }, { script: 'super' }],         // subscript/superscript
          [{ indent: '-1' }, { indent: '+1' }],             // thụt lề tăng/giảm
          [{ direction: 'rtl' }],                           // hướng viết phải–trái
          [{ size: ['small', false, 'large', 'huge'] }],    // chọn cỡ chữ
          [{ header: [1, 2, 3, 4, 5, 6, false] }],           // chọn cấp tiêu đề
          [{ color: [] }, { background: [] }],              // chọn màu chữ & nền
          [{ font: [] }],                                   // font chữ
          [{ align: [] }],                                  // căn lề
          ['clean']                                         // xoá định dạng
        ]
      }
    })
  ]
};
