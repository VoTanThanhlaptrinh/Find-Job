import {ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { loggerInterceptor } from './interceptor/logger.interceptor';
import { errorInterceptor } from './interceptor/error.interceptor';
import { provideToastr } from 'ngx-toastr';
import {provideQuillConfig, QuillModule} from 'ngx-quill';
import hljs from 'highlight.js';
import {provideNativeDateAdapter} from '@angular/material/core';
import {JwtModule} from '@auth0/angular-jwt';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([loggerInterceptor, errorInterceptor])
    ),
    importProvidersFrom(JwtModule.forRoot({})),
    provideToastr({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      progressBar: true,
      closeButton: true,
    }),
    provideNativeDateAdapter(),
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
