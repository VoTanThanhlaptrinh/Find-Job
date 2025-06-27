import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import {BehaviorSubject, catchError, filter, switchMap, take, throwError} from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return auth.refreshToken$().pipe(
            switchMap(token => {
              isRefreshing = false;
              refreshTokenSubject.next(token);
              const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true});
              return next(cloned);
            }),
            catchError(err => {
              isRefreshing = false;
              auth.logout();
              return throwError(() => err);
            })
          );
        } else {
          // wait until refresh done
          return refreshTokenSubject.pipe(
            filter(t => t != null),
            take(1),
            switchMap(token => {
              const cloned = req.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true });
              return next(cloned);
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};

