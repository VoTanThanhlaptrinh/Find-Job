import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { AccountService } from '../services/account.service';
import { TokenService } from '../services/token.service';
import { NotifyMessageService } from '../services/notify-message.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const accountService = inject(AccountService);
  const tokenService = inject(TokenService);
  const notifyMessageService = inject(NotifyMessageService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        auth.refreshToken$().subscribe({
          next: (token) => {
            tokenService.setToken(token);
          },
          error: (err) => {
            notifyMessageService.error(err.error.message);
            accountService.logout();
          }
        })
      }
      notifyMessageService.error(error.error.message || 'An error occurred');
      return throwError(() => error);
    })
  );
};
