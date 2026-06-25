import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, startWith, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';


@Injectable({
  providedIn: 'root',
})
export class AccountService {
  private readonly url: string;
  private isOauth2 = signal<boolean | null>(null);
  readonly isOauth2$ = computed(() => this.isOauth2());

  constructor(
    private http: HttpClient,
    private utilities: UtilitiesService,
  ) {
    this.url = this.utilities.getURLDev();
  }

  activate(token: string): Observable<any> {
    return this.http.get(`${this.url}/account/activate/${token}`).pipe(take(1));
  }

  getGoogleLoginUrl(): Observable<any> {
    return this.http.get(`${this.url}/auth/google/url`).pipe(take(1));
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
    return this.http.get(url).pipe(take(1));
  }

  changePass(value: any): Observable<any> {
    return this.http
      .put<any>(`${this.url}/account/changePass`, value)
      .pipe(take(1));
  }

  checkOauth2(): void {
    this.http
      .get<any>(`${this.url}/account/checkOauth2`)
      .pipe(take(1), map((res) => res.data), catchError(() => of(false)))
      .subscribe({
        next: (res) => this.isOauth2.set(res)
      });
  }
}
