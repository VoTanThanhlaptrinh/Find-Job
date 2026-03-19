import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import {
  catchError,
  finalize,
  map,
  Observable,
  shareReplay,
  switchMap,
  take,
  throwError,
} from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';

let refreshRequest$: Observable<string | null> | null = null;

function extractTokenFromResponse(response: unknown): string | null {
  if (typeof response === 'string') {
    return response;
  }

  if (
    response &&
    typeof response === 'object' &&
    'data' in response &&
    typeof (response as { data?: unknown }).data === 'string'
  ) {
    return (response as { data: string }).data;
  }

  return null;
}

export const refreshTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const isRefreshRequest = req.url.includes('/auth/refreshToken');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isRefreshRequest) {
        return throwError(() => error);
      }

      if (!refreshRequest$) {
        refreshRequest$ = authService.refreshToken$().pipe(
          take(1),
          map((response) => extractTokenFromResponse(response)),
          finalize(() => {
            refreshRequest$ = null;
          }),
          shareReplay(1)
        );
      }

      return refreshRequest$.pipe(
        switchMap((nextToken) => {
          if (!nextToken) {
            tokenService.clearToken();
            authService.setLoggedIn(false);
            return throwError(() => error);
          }

          tokenService.setToken(nextToken);
          authService.setLoggedIn(true);

          return next(
            req.clone({
              setHeaders: {
                Authorization: `Bearer ${nextToken}`,
              },
            })
          );
        }),
        catchError((refreshError: HttpErrorResponse) => {
          tokenService.clearToken();
          authService.setLoggedIn(false);
          return throwError(() => refreshError);
        })
      );
    })
  );
};
