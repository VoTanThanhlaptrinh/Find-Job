import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from './services/auth.service';

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {
  
  let jwtToken = null;
  if (typeof window !== 'undefined' && window.localStorage) {
     jwtToken = localStorage.getItem('jwtToken');
  }
  if (jwtToken && jwtToken.split('.').length === 3) { 
    const clone = req.clone({
      setHeaders: {
        Authorization: `Bearer ${jwtToken}`
      }
    });
    return next(clone);
  }
  return next(req);
};
