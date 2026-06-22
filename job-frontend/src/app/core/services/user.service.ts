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
    this.http.get<any>(`${this.url}/users`).pipe(take(1)).subscribe({
      next: (res) => {
        this.userDetails.set(res.data);
      },
      error: (err) => {
        console.error('Error fetching user details:', err);
      }
    });
  }

  updateInfo(value: any, id: string | number = 'id'): Observable<any> {
    return this.http
      .put<any>(`${this.url}/users/${id}`, value)
      .pipe(take(1));
  }
}
