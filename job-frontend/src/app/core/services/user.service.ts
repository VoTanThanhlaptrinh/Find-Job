import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly url: string;

  constructor(
    private http: HttpClient,
    private utilities: UtilitiesService
  ) {
    this.url = this.utilities.getURLDev();
  }

  getDetails(id: string | number = 'id'): Observable<any> {
    return this.http.get(`${this.url}/users/${id}`);
  }

  updateInfo(value: any, id: string | number = 'id'): Observable<any> {
    return this.http
      .put<any>(`${this.url}/users/${id}`, value)
      .pipe(take(1));
  }
}
