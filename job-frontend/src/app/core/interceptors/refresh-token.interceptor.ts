import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
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
  const router = inject(Router);
  const isRefreshRequest = req.url.includes('/auth/refreshToken');
  const platformId = inject(PLATFORM_ID);

  const getLoginUrl = () => {
    const url = router.url;
    if (url.includes('/recruiter')) {
      return '/recruiter/login';
    } else if (url.includes('/admin')) {
      return '/admin/login';
    } else {
      return '/login';
    }
  };

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isRefreshRequest) {
        return throwError(() => error);
      }

      if (!isPlatformBrowser(platformId)) {
        return throwError(() => error);
      }

      if (!refreshRequest$) {
        refreshRequest$ = authService.refreshToken$().pipe(
          take(1),
          map((response) => extractTokenFromResponse(response)),
          catchError((refreshError: HttpErrorResponse) => {
            // Only catch errors from the refresh token request itself
            authService.logout(getLoginUrl());
            return throwError(() => refreshError);
          }),
          finalize(() => {
            refreshRequest$ = null;
          }),
          shareReplay(1)
        );
      }

      return refreshRequest$.pipe(
        switchMap((nextToken) => {
          if (!nextToken) {
            authService.logout(getLoginUrl());
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
        })
      );
    })
  );
};