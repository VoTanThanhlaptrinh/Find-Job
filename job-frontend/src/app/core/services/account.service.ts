import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, finalize, map, Observable, of, startWith, take } from 'rxjs';
import { Router } from '@angular/router';
import { TokenService } from './token.service';
import { UtilitiesService } from './utilities.service';

@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly url: string;

  constructor(
    private http: HttpClient,
    private router: Router,
    private tokenService: TokenService,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev();
  }

  logout(): void {
    this.http
      .get<any>(`${this.url}/account/logout`, { withCredentials: true })
      .pipe(
        take(1),
        finalize(() => {
          this.router.navigate(['/login']);
          this.tokenService.clearToken();
        })
      ).subscribe();
  }

  activate(token: string): Observable<any> {
    return this.http.get(`${this.url}/account/activate/${token}`);
  }

  getGoogleLoginUrl(): Observable<any> {
    return this.http.get(`${this.url}/account/url/google`);
  }

  sendCode(email: string): Observable<any> {
    return this.http.get(`${this.url}/account/code/${email}`).pipe(take(1));
  }

  checkRandom(random: string): Observable<boolean> {
    return this.http
      .get<any>(`${this.url}/account/checkRandom/${random}`)
      .pipe(
        take(1),
        map((res) => 200 === res.status),
        startWith(false)
      );
  }

  sendLink(email: string): Observable<any> {
    const url = `${this.url}/account/sendLink/${email}`;
    return this.http.get(url);
  }

  changePass(value: any): Observable<any> {
    return this.http
      .put<any>(`${this.url}/account/changePass`, value)
      .pipe(take(1));
  }

  checkOauth2(): Observable<any> {
    return this.http
      .get<any>(`${this.url}/account/checkOauth2`)
      .pipe(take(1), map((res) => res.data), catchError(() => of(false)));
  }
}
