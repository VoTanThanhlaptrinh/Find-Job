import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UtilitiesService } from './utilities.service';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private url: string;
  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  listJobsNewest(pageIndex: number, pageSize: number): Observable<any> {
    return this.http.get<any>(
      `${this.url}/job/pub/listJobsNewest/${pageIndex}/${pageSize}`
    );
  }

  getAmount(): Observable<any> {
    return this.http.get<any>(`${this.url}/job/pub/getAmount`);
  }

  getAddressCount() {
    return this.http.get<any>(`${this.url}/job/pub/getAddressCount`);
  }
  filterWithAddressTimeSalary(filter: any) {
    return this.http.post<any>(
      `${this.url}/job/pub/filterWithAddressTimeSalary`,
      filter
    );
  }
}
