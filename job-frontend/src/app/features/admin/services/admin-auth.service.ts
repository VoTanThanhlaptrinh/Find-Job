import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, take, throwError } from 'rxjs';
import { TokenService } from '../../../core/services/token.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { ApiResponse } from '../../../shared/models/api-response.model';
import {
  AdminLoginData,
  AdminLoginPayload,
  AdminLoginProfile,
  AdminLogoutData,
} from './admin-api.models';

@Injectable({
  providedIn: 'root',
})
export class AdminAuthService {
  private readonly url: string;
  
  // Internal state
  private readonly _adminProfile = signal<AdminLoginProfile | null>(null);
  private readonly _refreshToken = signal<string | null>(null);
  private readonly _isLoggingIn = signal(false);

  // Public computed signals
  readonly adminProfile = computed(() => this._adminProfile());
  readonly isLoggedIn = computed(() => !!this._adminProfile());
  readonly isLoggingIn = computed(() => this._isLoggingIn());

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
   * Handle Admin Logout
   */
  logout(): void {
    const refreshToken = this._refreshToken();
    this.http
      .post<ApiResponse<AdminLogoutData>>(`${this.url}/admin/auth/logout`, { refreshToken }, {
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
    
    localStorage.setItem('admin_session', JSON.stringify({
      profile: data.admin,
      refreshToken: data.refreshToken
    }));
  }

  private clearAdminSession(): void {
    this.tokenService.clearToken();
    this._adminProfile.set(null);
    this._refreshToken.set(null);
    localStorage.removeItem('admin_session');
  }

  private loadAdminSession(): void {
    const stored = localStorage.getItem('admin_session');
    if (stored) {
      try {
        const session = JSON.parse(stored);
        this._adminProfile.set(session.profile);
        this._refreshToken.set(session.refreshToken);
        // Lưu ý: Access token thường được khôi phục qua cơ chế khác hoặc cần gọi refresh ngay nếu hết hạn
      } catch (e) {
        this.clearAdminSession();
      }
    }
  }
}
