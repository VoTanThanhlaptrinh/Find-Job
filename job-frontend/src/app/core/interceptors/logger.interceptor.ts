import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {
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
