import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import {
  catchError,
  Observable,
  of,
  startWith,
  take,
  throwError,
} from 'rxjs';
import { filter, map } from 'rxjs/operators';
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

  username = signal('');
  public isLoginClicked = computed(() => this.loginClick());
  public isRegisterClicked = computed(() => this.registerClick());
  private loggedIn: boolean = false;
  constructor(
    private http: HttpClient,
    private router: Router,
    private tokenService: TokenService,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev()
  }

  checkPageAccess() {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => {
        const navigationEvent = event as NavigationEnd;
        const url = navigationEvent.urlAfterRedirects;
        this.loginClick.set(url === '/login');
        this.registerClick.set(url === '/register');
      }
      ).unsubscribe();
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
          this.router.navigate(['/hirer']).then(() => window.location.reload());
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
    return this.http.get<any>(url, { withCredentials: true });
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
  isLogin(): boolean {
    this.http.get<ApiResponse<string>>(`${this.url}/auth/checkLogin`, { withCredentials: true }).pipe(
      take(1),
      map(res => res.data),
      catchError(() => of(false))
    ).subscribe((result) => {
      this.loggedIn = (result && this.tokenService.getTokenSubject() === result) ? true : false;
    });
    return this.loggedIn;
  }
}
