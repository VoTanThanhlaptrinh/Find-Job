import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class JobServiceService {

  constructor(private http: HttpClient) { }

  getDetailJob(id: string) :Observable<any> {
    return this.http.get(`http://localhost:8080/api/job/detail/${id}`);
  }
  checkApplyJob(id: string): Observable<any> {
    return this.http.get(`http://localhost:8080/api/job/check-apply/${id}`);
  }
}
