import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  constructor(private http: HttpClient) { }

  listJobsNewest(pageIndex:number,pageSize:number): Observable<any>{
    return this.http.get<any>(`http://localhost:8080/api/job/pub/listJobsNewest/${pageIndex}/${pageSize}`,);
  }

  getAmount():Observable<any> {
    return this.http.get<any>(`http://localhost:8080/api/job/pub/getAmount`);
  }

  getAddressCount() {
    return this.http.get<any>(`http://localhost:8080/api/job/pub/getAddressCount`);
  }
  filterWithAddressTimeSalary(filter:any){
    return this.http.post<any>(`http://localhost:8080/api/job/pub/filterWithAddressTimeSalary`,filter);
  }
}
