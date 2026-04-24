import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import {
  catchError,
  finalize,
  Observable,
  of,
  startWith,
  take,
  timeout,
  throwError,
} from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { ApiResponse } from '../../shared/models/api-response.model';
import { TokenService } from './token.service';
import { UtilitiesService } from './utilities.service';

interface RegisterResult {
  status: boolean;
  email: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  url = '';
  loginClick = signal(false);
  registerClick = signal(false);
  private readonly loggedIn = signal(false);
  private readonly _authReady = signal(false);
  private pageAccessTrackingInitialized = false;

  username = signal('');
  public isLoginClicked = computed(() => this.loginClick());
  public isRegisterClicked = computed(() => this.registerClick());
  public isLoggedIn = computed(() => this.loggedIn());
  public isAuthReady = computed(() => this._authReady());

  constructor(
    private http: HttpClient,
    private router: Router,
    private tokenService: TokenService,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev();
    this.checkPageAccess();
  }

  checkPageAccess() {
    if (this.pageAccessTrackingInitialized) {
      return;
    }

    this.pageAccessTrackingInitialized = true;
    this.loginClick.set(this.router.url === '/login');
    this.registerClick.set(this.router.url === '/register');

    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navigationEvent = event as NavigationEnd;
        const url = navigationEvent.urlAfterRedirects;
        this.loginClick.set(url === '/login');
        this.registerClick.set(url === '/register');
      });
  }

  login(body: any) {
    this.http
      .post<ApiResponse<string>>(`${this.url}/auth/login`, body, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => {
          this.tokenService.setToken(res.data);
          this.setLoggedIn(true);
        })
      )
      .subscribe();
  }

  hirerLogin(body: any) {
    return this.http
      .post<any>(`${this.url}/auth/login`, body, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => {
          this.tokenService.setToken(res.data);
          this.setLoggedIn(true);
          this.router.navigate(['/hirer']);
        }),
        catchError((err) => {
          const msg = err?.error?.message || 'Login failed';
          return throwError(() => msg);
        })
      );
  }

  register(data: any): Observable<RegisterResult> {
    return this.http.post<any>(`${this.url}/auth/register`, data).pipe(
      take(1),
      map((res) => ({
        status: res.status === 200,
        email: data.email,
      })),
      startWith({
        status: false,
        email: data.email,
      }),
      catchError((err) => {
        const msg = err?.error?.message || 'Dang ky that bai';
        return throwError(() => msg);
      })
    );
  }

  refreshToken$(): Observable<any> {
    const url = `${this.url}/auth/refreshToken`;
    return this.http.get<any>(url, { withCredentials: true }).pipe(take(1));
  }

  refreshToken() {
    const refreshEndpoint = `${this.url}/auth/refreshToken`;

    this.logAuthDebug('Starting refresh token request', {
      currentUrl: this.router.url,
      refreshEndpoint,
    });

    return this.http.get<any>(refreshEndpoint, { withCredentials: true }).pipe(
      timeout(8000),
      tap({
        next: (res) => {
          const token = typeof res?.data === 'string' ? res.data : '';
          this.tokenService.setToken(token);
          this.setLoggedIn(true);
          this.logAuthDebug('Refresh token succeeded', {
            currentUrl: this.router.url,
            hasToken: !!token,
            roles: this.tokenService.getTokenRoles(token),
          });
        },
        error: (error) => {
          this.setLoggedIn(false);
          this.logAuthDebug('Refresh token failed', {
            currentUrl: this.router.url,
            redirectCandidate: this.resolveLoginRoute(this.router.url),
            errorStatus: this.extractErrorStatus(error),
            errorMessage: this.extractErrorMessage(error),
          });
        },
      }),
      catchError((error) => {
        console.error('Refresh token failed during app startup', error);
        return of(null);
      }),
      finalize(() => {
        this._authReady.set(true);
        this.logAuthDebug('Auth state marked ready', {
          currentUrl: this.router.url,
          loggedIn: this.loggedIn(),
          roles: this.tokenService.getTokenRoles(),
          authReady: this._authReady(),
        });
      })
    );
  }

  markAuthReady(): void {
    this._authReady.set(true);
  }

  logout(): void {
    this.http
      .get<any>(`${this.url}/auth/logout`, { withCredentials: true })
      .pipe(
        take(1),
        finalize(() => {
          this.tokenService.clearToken();
          this.loggedIn.set(false);
        })
      ).subscribe();
  }

  forgotPass(form: any): Observable<any> {
    return this.http.post(`${this.url}/auth/forgotPassword`, form).pipe(take(1));
  }

  resetPass(form: any): Observable<any> {
    return this.http.patch(`${this.url}/auth/resetPassword`, form).pipe(take(1));
  }

  hasAnyRole(roles: string[], expectedRole: any) {
    return roles?.includes(expectedRole);
  }

  setLoggedIn(value: boolean): void {
    const previousValue = this.loggedIn();
    this.loggedIn.set(value);

    if (previousValue !== value) {
      this.logAuthDebug('Logged-in state changed', {
        previousValue,
        nextValue: value,
        currentUrl: this.router.url,
        roles: this.tokenService.getTokenRoles(),
      });
    }
  }

  isLogin(): boolean {
    return this.loggedIn();
  }

  private resolveLoginRoute(url: string): string {
    const path = this.normalizePath(url);

    if (path === '/recruiter' || path.startsWith('/recruiter/')) {
      return '/recruiter/login';
    }

    if (path === '/admin' || path.startsWith('/admin/')) {
      return '/admin/login';
    }

    return '/login';
  }

  private normalizePath(url: string): string {
    return (url || '/').split('?')[0].split('#')[0];
  }

  private extractErrorStatus(error: unknown): number | null {
    if (
      error &&
      typeof error === 'object' &&
      'status' in error &&
      typeof (error as { status?: unknown }).status === 'number'
    ) {
      return (error as { status: number }).status;
    }

    return null;
  }

  private extractErrorMessage(error: unknown): string | null {
    if (
      error &&
      typeof error === 'object' &&
      'message' in error &&
      typeof (error as { message?: unknown }).message === 'string'
    ) {
      return (error as { message: string }).message;
    }

    if (
      error &&
      typeof error === 'object' &&
      'error' in error &&
      (error as { error?: unknown }).error &&
      typeof (error as { error: { message?: unknown } }).error.message === 'string'
    ) {
      return (error as { error: { message: string } }).error.message;
    }

    return null;
  }

  private logAuthDebug(message: string, context?: Record<string, unknown>): void {
    console.info(`[AuthService] ${message}`, context ?? {});
  }
}
