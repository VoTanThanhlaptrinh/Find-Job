import { HttpClient, HttpEvent, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take, tap } from 'rxjs';
import { NotifyMessageService } from './notify-message.service';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private url = "http://localhost:8080/api/account/login"
  constructor(private http: HttpClient
              ,private toastr: NotifyMessageService) { }
  login(username: string, password: string): Observable<any>{
    const body = { username, password };
    return this.http.post<any>(this.url, body);
  }
  isLogin(): boolean {
    let token;
    let isLogin = false;
    if (typeof window !== 'undefined' && window.localStorage) {
      token = localStorage.getItem('jwtToken');
      return !!token;
    }
    if(token !== null && token !== undefined){
      console.log(token);
      this.http.get<any>(`http://localhost:8080/api/account/checkLogin/${token}`).pipe(take(1)).subscribe({
      next: (response) => {
        if (response.status === 200) {
          isLogin = true;
        } else {
          isLogin = false;
        }
      },
      error: (error) => {
        isLogin = false;
      }
    });
    }
    if(!isLogin){
      this.logout();
    }
    return isLogin;
  }
  
  logout(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      localStorage.removeItem('jwtToken');
    }
  }
  register(data: any): Observable<any> {
    const url = 'http://localhost:8080/api/account/register';
    return this.http.post(url, data);
  }
  sendLink(email: string): Observable<any> {
    const url = `http://localhost:8080/api/account/sendLink/${email}`;
    return this.http.get(url);
  }
  activate(token: string): Observable<any> {
    const url = `http://localhost:8080/api/account/activate/${token}`;
    return this.http.get(url);
  }
  refreshToken$(): Observable<string> {
    const url = 'http://localhost:8080/api/account/refreshToken';
    return this.http.get<{ data: string }>(url).pipe(
      take(1),
      tap((response: { data: string }) => {
        localStorage.setItem('jwtToken', response.data);
      }),
      map((response: { data: string }) => response.data)
    );
  }

}
