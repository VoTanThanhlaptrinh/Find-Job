import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenService } from '../services/token.service';
import { NO_AUTH } from './logger.interceptor';

function extractBearerToken(authorizationHeader: string | null): string {
  if (!authorizationHeader) {
    return '';
  }

  return authorizationHeader.replace(/^Bearer\s+/i, '').trim();
}

function isTerminalRefreshAuthError(error: unknown): boolean {
  if (!error || typeof error !== 'object' || !('status' in error)) {
    return false;
  }

  const status = (error as { status?: unknown }).status;
  return typeof status === 'number' && [400, 401, 403, 404].includes(status);
}

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
  const isNoAuthRequest = req.context.get(NO_AUTH) || req.url.includes('cloudflarestorage.com');
  const platformId = inject(PLATFORM_ID);
  const sentToken = extractBearerToken(req.headers.get('Authorization'));

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isRefreshRequest || isNoAuthRequest) {
        return throwError(() => error);
      }

      if (!isPlatformBrowser(platformId)) {
        return throwError(() => error);
      }

      const latestToken = tokenService.getToken();
      if (latestToken && latestToken !== sentToken) {
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${latestToken}`,
            },
          })
        );
      }

      return authService.refreshToken$().pipe(
        catchError((refreshError: unknown) => {
          if (isTerminalRefreshAuthError(refreshError)) {
            authService.logout(authService.getLoginUrl());
          }
          return throwError(() => refreshError);
        }),
        switchMap((response) => {
          const nextToken = extractTokenFromResponse(response);
          if (!nextToken) {
            authService.logout(authService.getLoginUrl());
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