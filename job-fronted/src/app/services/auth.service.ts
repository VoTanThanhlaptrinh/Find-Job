import { HttpClient, HttpEvent, HttpResponse } from '@angular/common/http';
import {Inject, Injectable, PLATFORM_ID} from '@angular/core';
import {BehaviorSubject, catchError, finalize, Observable, of, startWith, take, tap, throwError} from 'rxjs';
import { NotifyMessageService } from './notify-message.service';
import { map } from 'rxjs/operators';
import {Router} from '@angular/router';
import {isPlatformBrowser} from '@angular/common';

interface RegisterResult{
  status: boolean
  email: string
}

@Injectable({
  providedIn: 'root'
})

export class AuthService {
  token = '';

  constructor(private http: HttpClient
              ,private notifyMessageService: NotifyMessageService
              ,private router: Router
              ,@Inject(PLATFORM_ID) private _platformId: Object,) {
    this.checkLogin()
  }
  login(body:any): Observable<any>{
    return this.http.post<any>("http://localhost:8080/api/account/pub/login", body, {withCredentials: true}).pipe(take(1),map(res =>{
            localStorage.setItem("jwtToken",res.data);
    }),catchError((err) =>{
        const msg = err?.error?.message || 'Login failed';
        return throwError(() => msg);
    }));
  }
  checkLogin(): boolean {
    if(isPlatformBrowser(this._platformId)){
      return localStorage.getItem('jwtToken') !== null;
    }
    return false;
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
    return this.http.post<any>('http://localhost:8080/api/account/pub/register', data).pipe(
      map(res => ({
          status: res.status === 200,
          email: data.email
      }))
      , startWith({
        status: false,
        email: data.email
      })
      , catchError(err => {
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
    return this.http.get<any>(url, {withCredentials: true}).pipe(
      take(1),
      tap((response: any) => {
        localStorage.setItem('jwtToken', response.data);
      }),
      map((response: any) => response)
    );
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
}
