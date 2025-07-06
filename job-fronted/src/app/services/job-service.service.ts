import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable, take} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class JobServiceService {

  constructor(private http: HttpClient) { }

  getDetailJob(id: string) :Observable<any> {
    return this.http.get(`http://localhost:8080/api/job/pub/detail/${id}`).pipe(take(1));
  }
  checkApplyJob(id: string): Observable<any> {
    return this.http.get(`http://localhost:8080/api/job/pub/check-apply/${id}`).pipe(take(1));
  }
  doPostJob(form:any): Observable<any>{
    return this.http.post('http://localhost:8080/api/job/pri/postJob',form, {withCredentials: true}).pipe(take(1));
  }

  getHirerJobPost(pageIndex:number, pageSize:number) {
    return this.http.get<any>(`http://localhost:8080/api/job/pri/h/hirerJobPost/${pageIndex}/${pageSize}`, {withCredentials: true}).pipe(take(1));
  }

  countHirerJobPost() {
    return this.http.get<any>(`http://localhost:8080/api/job/pri/h/countHirerJobPost`, {withCredentials: true}).pipe(take(1));

  }
}
