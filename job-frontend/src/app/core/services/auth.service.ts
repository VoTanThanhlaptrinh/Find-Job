import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import {
  catchError,
  finalize,
  Observable,
  of,
  startWith,
  take,
  throwError,
} from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';
import { NavigationEnd, Router } from '@angular/router';
import { TokenService } from './token.service';
import { UtilitiesService } from './utilities.service';
import { ApiResponse } from '../../shared/models/api-response.model';

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
  private loginStatusRequestInFlight = false;

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
      ).subscribe();
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
    return this.http
      .post<any>(`${this.url}/auth/register`, data)
      .pipe(
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
          const msg = err?.error?.message || 'Đăng ký thất bại';
          return throwError(() => msg);
        })
      );
  }
  refreshToken$(): Observable<any> {
    const url = `${this.url}/auth/refreshToken`;
    return this.http.get<any>(url, { withCredentials: true }).pipe(take(1));
  }
  refreshToken() {
    return this.http.get<any>(`${this.url}/auth/refreshToken`, { withCredentials: true }).pipe(
      tap({
        next: (res) => {
          this.tokenService.setToken(res.data);
          this.setLoggedIn(true);
        },
        error: () => {
          this.setLoggedIn(false);
        }
      }),
      catchError((error) => {
        console.error('Lỗi khi lấy refresh token lúc khởi động', error);
        return of(null);
      }),
      finalize(() => {
        this._authReady.set(true);
      })
    );
  }
  logout(): void {
    this.http
      .get<any>(`${this.url}/auth/logout`, { withCredentials: true })
      .pipe(
        take(1),
        finalize(() => {
          this.router.navigate(['/login']);
          this.tokenService.clearToken();
          this.loggedIn.set(false);
        })
      ).subscribe();
  }
  forgotPass(form: any): Observable<any> {
    return this.http
      .post(`${this.url}/auth/forgotPassword`, form)
      .pipe(take(1));
  }
  resetPass(form: any): Observable<any> {
    return this.http.patch(`${this.url}/auth/resetPassword`, form).pipe(take(1));
  }
  hasAnyRole(roles: string[], expectedRole: any) {
    return roles?.includes(expectedRole);
  }
  setLoggedIn(value: boolean): void {
    this.loggedIn.set(value);
  }
  isLogin(): boolean {
    return this.loggedIn();
  }
}
