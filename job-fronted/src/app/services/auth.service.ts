import { HttpClient, HttpEvent, HttpResponse } from '@angular/common/http';
import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {BehaviorSubject, catchError, finalize, Observable, of, startWith, take, tap, throwError} from 'rxjs';
import { NotifyMessageService } from './notify-message.service';
import { map } from 'rxjs/operators';
import {Router} from '@angular/router';
import {isPlatformBrowser} from '@angular/common';
import {JwtHelperService} from '@auth0/angular-jwt';

interface RegisterResult{
  status: boolean
  email: string
}

@Injectable({
  providedIn: 'root'
})

export class AuthService {
  token = '';
  private loggedIn: boolean = false
  constructor(private http: HttpClient
              ,private notifyMessageService: NotifyMessageService
              ,private router: Router
              ,private jwtHelper: JwtHelperService
              ,@Inject(PLATFORM_ID) private _platformId: Object,) {
  }
  login(body:any): Observable<any> {
    if(body.role === 'USER'){
      return this.http.post<any>("http://localhost:8080/api/account/pub/u/login", body, {withCredentials: true}).pipe(take(1),map(res =>{
        localStorage.setItem("jwtToken",res.data);
        window.location.reload()
      }),catchError((err) =>{
        const msg = err?.error?.message || 'Login failed';
        return throwError(() => msg);
      }));
    }else{
      return this.http.post<any>("http://localhost:8080/api/account/pub/h/login", body, {withCredentials: true}).pipe(take(1),map(res =>{
        localStorage.setItem("jwtToken",res.data);
        window.location.reload()
      }),catchError((err) =>{
        const msg = err?.error?.message || 'Login failed';
        return throwError(() => msg);
      }));
    }
  }
  checkUserLogin(): Observable<boolean> {
    if (isPlatformBrowser(this._platformId)) {
      this.token = localStorage.getItem('jwtToken') || '';
    }
    if (!this.token) {
      return of(false);
    }
    return this.http
      .get<any>('http://localhost:8080/api/account/pri/u/checkLogin', { withCredentials: true })
      .pipe(
        take(1),
        map((res:any) => res.status === 200),           // nếu thành công trả về true
        catchError(() => of(false))  // nếu lỗi trả về false
      );
  }

  checkHirerLogin(): Observable<boolean> {
    if (isPlatformBrowser(this._platformId)) {
      this.token = localStorage.getItem('jwtToken') || '';
    }
    if (!this.token) {
      return of(false);
    }
    return this.http
      .get<any>('http://localhost:8080/api/account/pri/h/checkLogin', { withCredentials: true })
      .pipe(
        take(1),
        map((res:any) => res.status === 200),
        catchError(() => of(false))
      );
  }
  logout() {
    return this.http.get<any>('http://localhost:8080/api/account/pub/logout',{withCredentials: true})
      .pipe(take(1),finalize(() =>{
        if (typeof window !== 'undefined' && window.localStorage) {
          localStorage.removeItem('jwtToken');
          this.router.navigate(['/login'])
        }
      }));
  }
  register(data: any): Observable<RegisterResult> {
    if(data.role === 'USER'){
      return this.http.post<any>('http://localhost:8080/api/account/pub/u/register', data).pipe(
        map(res => ({
          status: res.status === 200,
          email: data.email
        })),startWith({
          status: false,
          email: data.email
        }),catchError(err => {
          const msg = err?.error?.message || 'Đăng ký thất bại';
          return throwError(() => msg);
        }));
    }
    return this.http.post<any>('http://localhost:8080/api/account/pub/h/register', data).pipe(
      map(res => ({
        status: res.status === 200,
        email: data.email
      })),startWith({
        status: false,
        email: data.email
      }),catchError(err => {
        const msg = err?.error?.message || 'Đăng ký thất bại';
        return throwError(() => msg);
      }));
  }
  sendLink(email: string): Observable<any> {
    const url = `http://localhost:8080/api/account/sendLink/${email}`;
    return this.http.get(url);
  }
  activate(token: string): Observable<any> {
    const url = `http://localhost:8080/api/account/pub/activate/${token}`;
    return this.http.get(url);
  }
  refreshToken$(): Observable<any> {
    const url = 'http://localhost:8080/api/account/pub/refreshToken';
    return this.http.get<any>(url, {withCredentials: true}).pipe(take(1));
  }
  getDetails():Observable<any>{
    return this.http.get('http://localhost:8080/api/account/pri/detail',{withCredentials: true});
  }
  getGoogleLoginUrl():Observable<any> {
    return this.http.get('http://localhost:8080/api/account/pub/url/google');
  }
  fetchToken(token: string) {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.setItem("jwtToken",token);
    }
  }
  sendCode(email:string): Observable<any>{
    return this.http.get(`http://localhost:8080/api/account/pub/code/${email}`).pipe(take(1));
  }
  forgotPass(form:any): Observable<any>{
    return this.http.post('http://localhost:8080/api/account/pub/forgotPass',form).pipe(take(1))
  }
  resetPass(form:any):Observable<any>{
    return this.http.patch('http://localhost:8080/api/account/pub/reset',form).pipe(take(1))
  }

  checkRandom(random: string): Observable<boolean> {
    return this.http.get<any>(`http://localhost:8080/api/account/pub/checkRandom/${random}`)
      .pipe(take(1)
            ,map(res => 200 === res.status)
            ,startWith(false));
  }
  updateInfo(value: any) {
    return this.http.put<any>('http://localhost:8080/api/account/pri/updateUserInfo',value).pipe(take(1));
  }

  changePass(value:any) {
    return this.http.put<any>('http://localhost:8080/api/account/pri/changePass',value).pipe(take(1));
  }

  checkOauth2() {
    return this.http.get<any>('http://localhost:8080/api/account/pri/checkOauth2').pipe(take(1));
  }
  public isAuthenticated(): boolean {
    const token = localStorage.getItem('jwtToken');
    return !this.jwtHelper.isTokenExpired(token);
  }


  hasAnyRole(roles: string[], expectedRole: any) {
    return roles?.includes(expectedRole);
  }
}
