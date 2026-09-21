import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

export const NO_AUTH = new HttpContextToken<boolean>(() => false);
export const SKIP_AUTH = new HttpContextToken<boolean>(() => false);
export const IS_PUBLIC = new HttpContextToken<boolean>(() => false);

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {
  // 1. Kiểm tra tham số HttpContext (NO_AUTH / SKIP_AUTH / IS_PUBLIC)
  if (req.context.get(NO_AUTH) || req.context.get(SKIP_AUTH) || req.context.get(IS_PUBLIC)) {
    return next(req);
  }

  // 2. Kiểm tra header chỉ định bỏ qua token (nếu có)
  if (req.headers.has('Skip-Auth') || req.headers.has('No-Auth') || req.headers.has('anonymous')) {
    const cleanHeaders = req.headers
      .delete('Skip-Auth')
      .delete('No-Auth')
      .delete('anonymous');
    return next(req.clone({ headers: cleanHeaders }));
  }

  // 3. Tự động bỏ qua request đến Cloudflare R2 / AWS S3 presigned URL
  if (req.url.includes('cloudflarestorage.com') || req.url.includes('.r2.') || req.url.includes('amazonaws.com')) {
    return next(req);
  }

  const tokenService = inject(TokenService);
  let jwtToken = tokenService.getToken();
  if (jwtToken) {
    const clone = req.clone({
      setHeaders: {
        Authorization: `Bearer ${jwtToken}`,
      },
    });
    return next(clone);
  }
  return next(req);
};
