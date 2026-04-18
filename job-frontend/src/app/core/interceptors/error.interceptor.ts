import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { NotifyMessageService } from '../services/notify-message.service';
import { AuthService } from '../services/auth.service';

let isForceLoggingOut = false;

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifyMessageService = inject(NotifyMessageService);
  const authService = inject(AuthService);
  const iRefreshsRequest = req.url.includes('/auth/refreshToken');
  const isLogoutRequest = req.url.includes('/auth/logout');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 403 && !iRefreshsRequest && !isLogoutRequest && !isForceLoggingOut) {
        isForceLoggingOut = true;
        authService.logout();
        setTimeout(() => {
          isForceLoggingOut = false;
        }, 0);
      }

      if (!iRefreshsRequest) {
        const message =
          error?.error?.message ||
          error?.message ||
          'An error occurred';
        notifyMessageService.error(message);
      }

      return throwError(() => error);
    })
  );
};
