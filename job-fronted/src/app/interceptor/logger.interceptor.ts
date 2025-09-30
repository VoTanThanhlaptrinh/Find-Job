import { HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  let jwtToken = auth.getJwtToken();
  if (jwtToken) {
    const clone = req.clone({
    setHeaders: {
      Authorization: `Bearer ${jwtToken}`,
    },});
    return next(clone);
  }
  return next(req);
};
