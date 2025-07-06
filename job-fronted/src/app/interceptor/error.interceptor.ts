import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import {BehaviorSubject, catchError, filter, finalize, switchMap, take, throwError} from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);
          return auth.refreshToken$().pipe(
            take(1),
            switchMap(token => {
              refreshTokenSubject.next(token);
              localStorage.setItem('jwtToken',token)
              return next(req.clone({
                setHeaders: { Authorization: `Bearer ${token}` },
                withCredentials: true
              }));
            }),
            finalize(() => { isRefreshing = false; refreshTokenSubject = new BehaviorSubject<string|null>(null) }),
            catchError(err => {
              auth.logout().subscribe();
              window.location.reload();
              return throwError(() => err);
            })
          );
        }
        return refreshTokenSubject.pipe(
          filter(t => t != null),
          take(1),
          switchMap(token => next(req.clone({
            setHeaders: { Authorization: `Bearer ${token}` },
            withCredentials: true
          })))
        );
      }
      return throwError(() => error);
    })
  );
};
