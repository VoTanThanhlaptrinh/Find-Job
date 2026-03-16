import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
@Injectable({
  providedIn: 'root',
})
export class HomeService {
  private url :string;
  constructor(private http: HttpClient,
              private utilities: UtilitiesService,
  ) {
    this.url = utilities.getURLDev()
  }
  getData(): Observable<any> {
    return this.http.get<any>(`${this.url}/home/init`).pipe(take(1));
  }
}
