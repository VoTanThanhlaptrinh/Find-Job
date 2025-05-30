import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from './services/auth.service';
import e from 'express';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const http = inject(HttpClient);
  return next(req).pipe(
    catchError((error) => {
      if (error.status === 403 && 'token invalid' === error.error.message) {
        return authService.refreshToken$().pipe(
          switchMap((newToken: string) => {
            const clonedRequest = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
          return next(clonedRequest);
          })
          ,catchError((error) => {
            authService.logout();
            console.error('Error refreshing token:', error);
            return throwError(() => null);
          }));
      }
      return throwError(() => null);
    }));
};
