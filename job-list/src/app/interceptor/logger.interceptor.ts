import { HttpInterceptorFn } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

export const loggerInterceptor: HttpInterceptorFn = (req, next) => {

  let jwtToken = null;
  if (typeof window !== 'undefined' && window.localStorage) {
     jwtToken = localStorage.getItem('jwtToken');
  }
  // if (jwtToken) {
  //   const clone = req.clone({
  //     setHeaders: {
  //       Authorization: `Bearer ${jwtToken}`
  //     }
  //   });
  //   return next(clone);
  // }
  // console.log('[Logger] Token =', jwtToken, 'for', req.method, req.url);
  // return next(req);
  const clone = jwtToken ? req.clone({ setHeaders: { Authorization: `Bearer ${jwtToken}` }, withCredentials:true }) : req;
  return next(clone);
};
