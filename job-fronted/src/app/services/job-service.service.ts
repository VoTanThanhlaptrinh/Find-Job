import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {Observable, take} from 'rxjs';
import { UtilitiesService } from './utilities.service';

@Injectable({
  providedIn: 'root'
})
export class JobServiceService {
  private url: string;
  constructor(private http: HttpClient,
            private utilities: UtilitiesService
  ) { 
    this.url = utilities.getURLDev()
  }

  getDetailJob(id: string) :Observable<any> {
    return this.http.get(`${this.url}/job/pub/detail/${id}`).pipe(take(1));
  }
  checkApplyJob(id: string): Observable<any> {
    return this.http.get(`${this.url}/job/pub/check-apply/${id}`).pipe(take(1));
  }
  doPostJob(form:any): Observable<any>{
    return this.http.post(`${this.url}/job/pri/postJob`,form, {withCredentials: true}).pipe(take(1));
  }

  getHirerJobPost(pageIndex:number, pageSize:number) {
    return this.http.get<any>(`${this.url}/job/pri/h/hirerJobPost/${pageIndex}/${pageSize}`, {withCredentials: true}).pipe(take(1));
  }

  countHirerJobPost() {
    return this.http.get<any>(`${this.url}/job/pri/h/countHirerJobPost`, {withCredentials: true}).pipe(take(1));
  }
    filterWithAddressTimeSalary(filter:any){
    return this.http.post<any>(`${this.url}/job/pub/filterWithAddressTimeSalary`,filter);
  }
}
