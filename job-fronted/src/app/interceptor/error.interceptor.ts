import {HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import {BehaviorSubject, catchError, filter, finalize, switchMap, take, throwError} from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if(error.status === 401  && error.error.data === 'cookie-expired'){
          auth.logout().subscribe();
          return throwError(() => error);
      }
      if (error.status === 401) {
          return auth.refreshToken$().pipe(
            switchMap(token => {
              auth.setJwtToken(token);
              return next(req.clone({
                setHeaders: { Authorization: `Bearer ${token}` },
                withCredentials: true
              }));
            }),
            catchError(err => {
              auth.logout().subscribe();
              return throwError(() => err);
            })
          );
      }
      return throwError(() => error);
    })
  );
};
