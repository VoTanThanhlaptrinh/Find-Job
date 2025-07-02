import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable, take} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HomeService {

  constructor(private http: HttpClient) { }
  private url = "http://localhost:8080/api/home/init"
  getData(): Observable<any> {
    return this.http.get<any>(this.url).pipe(take(1)) ;
  }
}
