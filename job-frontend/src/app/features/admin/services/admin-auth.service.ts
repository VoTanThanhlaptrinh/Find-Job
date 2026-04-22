import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { computed, Injectable, inject, PLATFORM_ID, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, take, throwError } from 'rxjs';
import { TokenService } from '../../../core/services/token.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminLoginData,
  AdminLoginPayload,
  AdminLoginProfile,
  AdminLogoutData,
  AdminRefreshData,
} from './admin-api.models';

@Injectable({
  providedIn: 'root',
})
export class AdminAuthService {
  private readonly url: string;
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  
  // Internal state
  private readonly _adminProfile = signal<AdminLoginProfile | null>(null);
  private readonly _refreshToken = signal<string | null>(null);
  private readonly _isLoggingIn = signal(false);
  private readonly _isRefreshing = signal(false);

  // Public computed signals
  readonly adminProfile = computed(() => this._adminProfile());
  readonly isLoggedIn = computed(() => !!this._adminProfile());
  readonly isLoggingIn = computed(() => this._isLoggingIn());
  readonly isRefreshing = computed(() => this._isRefreshing());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly tokenService: TokenService,
    private readonly notify: NotifyMessageService,
    private readonly router: Router
  ) {
    this.url = this.utilities.getURLDev();
    this.loadAdminSession();
  }

  /**
   * Handle Admin Login
   */
  login(payload: AdminLoginPayload): Observable<void> {
    this._isLoggingIn.set(true);
    return this.http
      .post<ApiResponse<AdminLoginData>>(`${this.url}/admin/auth/login`, payload, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res) => {
          const data = res.data;
          this.persistSession(data);
          this.notify.success('Đăng nhập quản trị viên thành công');
          this.router.navigate(['/admin/dashboard']);
        }),
        catchError((err) => {
          const msg = err?.error?.message || 'Đăng nhập thất bại';
          this.notify.error(msg);
          return throwError(() => msg);
        }),
        finalize(() => this._isLoggingIn.set(false))
      );
  }

  /**
   * Refresh current admin session token pair
   */
  refreshSession(silent: boolean = false): Observable<void> {
    const refreshToken = this._refreshToken();
    if (!refreshToken) {
      return of(void 0);
    }

    this._isRefreshing.set(true);
    return this.http
      .post<ApiResponse<AdminRefreshData>>(
        `${this.url}/admin/auth/refresh`,
        { refreshToken },
        { withCredentials: true }
      )
      .pipe(
        take(1),
        map((res) => {
          this.persistSession(res.data);
        }),
        catchError((err) => {
          if (!silent) {
            const msg = err?.error?.message || 'Lam moi phien dang nhap that bai';
            this.notify.error(msg);
          }
          this.clearAdminSession();
          return throwError(() => err);
        }),
        finalize(() => this._isRefreshing.set(false))
      );
  }

  /**
   * Handle Admin Logout
   */
  logout(): void {
    const refreshToken = this._refreshToken();
    const payloadRefreshToken = refreshToken ?? '';
    this.http
      .post<ApiResponse<AdminLogoutData>>(`${this.url}/admin/auth/logout`, { refreshToken: payloadRefreshToken }, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        finalize(() => {
          this.clearAdminSession();
          this.notify.info('Đã đăng xuất khỏi hệ thống quản trị');
          this.router.navigate(['/admin/login']);
        })
      )
      .subscribe();
  }

  /**
   * Internal session management
   */
  private persistSession(data: AdminLoginData): void {
    this.tokenService.setToken(data.accessToken);
    this._adminProfile.set(data.admin);
    this._refreshToken.set(data.refreshToken);

    if (this.isBrowser) {
      localStorage.setItem('admin_session', JSON.stringify({
        profile: data.admin,
        refreshToken: data.refreshToken
      }));
    }
  }

  private clearAdminSession(): void {
    this.tokenService.clearToken();
    this._adminProfile.set(null);
    this._refreshToken.set(null);

    if (this.isBrowser) {
      localStorage.removeItem('admin_session');
    }
  }

  private loadAdminSession(): void {
    if (!this.isBrowser) {
      return;
    }

    const stored = localStorage.getItem('admin_session');
    if (stored) {
      try {
        const session = JSON.parse(stored);
        this._adminProfile.set(session.profile);
        this._refreshToken.set(session.refreshToken);
        this.refreshSession(true).pipe(take(1)).subscribe({
          error: () => {
            // Errors are already handled in refreshSession.
          }
        });
      } catch (e) {
        this.clearAdminSession();
      }
    }
  }
}
