import { HttpClient, HttpEvent, HttpResponse } from '@angular/common/http';
import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import {
  BehaviorSubject,
  catchError,
  finalize,
  Observable,
  of,
  startWith,
  take,
  tap,
  throwError,
} from 'rxjs';
import { NotifyMessageService } from './notify-message.service';
import { map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';
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
  private loggedIn: boolean = false;
  constructor(
    private http: HttpClient,
    private notifyMessageService: NotifyMessageService,
    private router: Router,
    private jwtHelper: JwtHelperService,
    @Inject(PLATFORM_ID) private _platformId: Object,
    private tokenService: TokenService,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev()
  }
  login(body: any): Observable<any> {
    if (body.role === 'ROLE_USER') {
      return this.http
        .post<any>(`${this.url}/account/pub/u/login`, body, {
          withCredentials: true,
        })
        .pipe(
          take(1),
          map((res) => {
            this.tokenService.setToken(res.data);
            this.router.navigate(['/']).then(() =>{window.location.reload()});
          }),
          catchError((err) => {
            const msg = err?.error?.message || 'Login failed';
            return throwError(() => msg);
          })
        );
    }
    if (body.role === 'ROLE_HIRER') {
      return this.http
        .post<any>(`${this.url}/account/pub/h/login`, body, {
          withCredentials: true,
        })
        .pipe(
          take(1),
          map((res) => {
            this.tokenService.setToken(res.data);
            this.router.navigate(['/hirer']).then(() =>window.location.reload());
          }),
          catchError((err) => {
            const msg = err?.error?.message || 'Login failed';
            return throwError(() => msg);
          })
        );
    }
    return of(null);
  }
  checkLogin(){
    return this.http.get(`${this.url}/account/pri/checkLogin`, {
        withCredentials: true,
      }).pipe(
        take(1),
      map((res : any) => res.data),
      catchError(() => of(false))
    )
  }
  checkUserLogin(): Observable<boolean> {
    // kiểm tra xem trong token có jwt token không
    if (this.tokenService.getToken() === '') {
      return of(false);
    }
    // nếu có thì kiểm tra xem nó còn hạn sử dụng hay không
    return this.http
      .get<any>(`${this.url}/account/pri/u/isUser`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res: any) => res.data), 
        catchError(() => of(false)) // catch lỗi trả về false
      );
  }

  checkHirerLogin(): Observable<boolean> {
    if (this.tokenService.getToken() === '') {
      return of(false);
    }
    return this.http
      .get<any>(`${this.url}/account/pri/h/isHirer`, {
        withCredentials: true,
      })
      .pipe(
        take(1),
        map((res: any) => res.data),
        catchError(() => of(false))
      );
  }
  logout() {
    return this.http
      .get<any>(`${this.url}/account/pub/logout`, { withCredentials: true })
      .pipe(
        take(1),
        finalize(() => {
            this.router.navigate(['/login']);
            this.tokenService.clearToken();
        })
      );
  }
  register(data: any): Observable<RegisterResult> {
    if (data.role === 'ROLE_USER') {
      return this.http
        .post<any>(`${this.url}/account/pub/u/register`, data)
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
    return this.http.post<any>(`${this.url}/account/pub/h/register`, data).pipe(
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
  sendLink(email: string): Observable<any> {
    const url = `${this.url}/account/sendLink/${email}`;
    return this.http.get(url);
  }
  activate(token: string): Observable<any> {
    const url = `${this.url}/account/pub/activate/${token}`;
    return this.http.get(url);
  }
  refreshToken$(): Observable<any> {
    const url = `${this.url}/account/pub/refreshToken`;
    return this.http.get<any>(url, { withCredentials: true });
  }
  getDetails(): Observable<any> {
    return this.http.get(`${this.url}/account/pri/detail`, {
      withCredentials: true,
    });
  }
  getGoogleLoginUrl(): Observable<any> {
    return this.http.get(`${this.url}/account/pub/url/google`);
  }
  sendCode(email: string): Observable<any> {
    return this.http.get(`${this.url}/account/pub/code/${email}`).pipe(take(1));
  }
  forgotPass(form: any): Observable<any> {
    return this.http
      .post(`${this.url}/account/pub/forgotPass`, form)
      .pipe(take(1));
  }
  resetPass(form: any): Observable<any> {
    return this.http.patch(`${this.url}/account/pub/reset`, form).pipe(take(1));
  }

  checkRandom(random: string): Observable<boolean> {
    return this.http
      .get<any>(`${this.url}/account/pub/checkRandom/${random}`)
      .pipe(
        take(1),
        map((res) => 200 === res.status),
        startWith(false)
      );
  }
  updateInfo(value: any) {
    return this.http
      .put<any>(`${this.url}/account/pri/updateUserInfo`, value)
      .pipe(take(1));
  }

  changePass(value: any) {
    return this.http
      .put<any>(`${this.url}/account/pri/changePass`, value)
      .pipe(take(1));
  }

  checkOauth2() {
    return this.http
      .get<any>(`${this.url}/account/pri/checkOauth2`)
      .pipe(take(1),map(res => res.data), catchError(() => of(false)));
  }
  public isAuthenticated(): boolean {
    return !this.jwtHelper.isTokenExpired(this.tokenService.getToken());
  }
  hasAnyRole(roles: string[], expectedRole: any) {
    return roles?.includes(expectedRole);
  }
  getJwtToken(): string {
    return this.tokenService.getToken();
  }
  setJwtToken(token: string): void {
    this.tokenService.setToken(token);
  }
}
