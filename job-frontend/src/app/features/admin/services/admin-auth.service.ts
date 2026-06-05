import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { computed, Injectable, inject, PLATFORM_ID, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, map, Observable, of, take, tap, throwError } from 'rxjs';
import { TokenService } from '../../../core/services/token.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import {
  AdminLoginData,
  AdminLoginPayload,
} from './admin-api.models';
import { ApiResponse } from '../../../shared/models/api-response.model';
import { AuthService } from '../../../core/services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AdminAuthService {
  private readonly url: string;
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  // Internal state
  private readonly _isLoggingIn = signal(false);
  private readonly _isRefreshing = signal(false);

  // Public computed signals
  readonly isLoggingIn = computed(() => this._isLoggingIn());
  readonly isRefreshing = computed(() => this._isRefreshing());

  constructor(
    private readonly http: HttpClient,
    private readonly utilities: UtilitiesService,
    private readonly tokenService: TokenService,
    private readonly notify: NotifyMessageService,
    private readonly router: Router,
    private readonly authService: AuthService
  ) {
    this.url = this.utilities.getURLDev();
  }

  /**
   * Handle Admin Login
   */
login(payload: AdminLoginPayload): Observable<void> {
  this._isLoggingIn.set(true);
  return this.http
    .post<ApiResponse<AdminLoginData>>(`${this.url}/auth/admin/login`, payload, {
      withCredentials: true,
    })
    .pipe(
      take(1),
      tap((res) => {
        this.persistSession(res.data);
        this.notify.success('Đăng nhập quản trị viên thành công');       
        this.router.navigateByUrl('/admin/dashboard');
      }),
      map(() => void 0), 
      catchError((err) => {
        const msg = err?.error?.message || 'Đăng nhập thất bại';
        this.notify.error(msg);
        return throwError(() => err);
      }),
      finalize(() => this._isLoggingIn.set(false))
    );
  }

  logout(): void {
    this.authService.logout();
  }

  private persistSession(data: AdminLoginData): void {
    this.tokenService.setToken(data.accessToken);
  }

  private clearAdminSession(): void {
    this.tokenService.clearToken();
  }

}
