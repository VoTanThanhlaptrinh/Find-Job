import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { NotifyMessageService } from '../services/notify-message.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifyMessageService = inject(NotifyMessageService);
  const isStatusRequest = req.url.includes('/auth/status');
  const iRefreshsRequest = req.url.includes('/auth/refreshToken');
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isStatusRequest) {
        const message =
          error?.error?.message ||
          error?.message ||
          'An error occurred';
        notifyMessageService.error(message);
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
