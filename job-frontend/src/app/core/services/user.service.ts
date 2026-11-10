import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly url: string;
  readonly userDetails = signal<any>(null);

  constructor(
    private http: HttpClient,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev();
  }

  getDetails(): void {
    this.http.get<any>(`${this.url}/account/profile`).pipe(take(1)).subscribe({
      next: (res) => {
        this.userDetails.set(res.data);
      },
      error: (err) => {
        console.error('Error fetching user details:', err);
      }
    });
  }

  updateInfo(value: any): Observable<any> {
    return this.http
      .put<any>(`${this.url}/account/profile`, value)
      .pipe(take(1));
  }
}
