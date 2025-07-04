import { HttpInterceptorFn } from '@angular/common/http';
import {Inject, inject, PLATFORM_ID} from '@angular/core';
import {AuthService} from '../services/auth.service';
import {isPlatformBrowser} from '@angular/common';

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const _platformId = inject(PLATFORM_ID);
  let jwtToken = null;
  if(isPlatformBrowser(_platformId)){
    jwtToken = localStorage.getItem('jwtToken');
  }
  if (jwtToken) {
    const clone = req.clone({
      setHeaders: {
        Authorization: `Bearer ${jwtToken}`
      }
    });
    return next(clone);
  }
  return next(req);
};
