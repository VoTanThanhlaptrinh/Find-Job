import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import {
  JobAddressCountApiResponse,
  JobCountApiResponse,
  JobFilterPayload,
  JobListApiResponse,
} from '../../../shared/models/jobs/job-api-response.model';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private url: string;

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  listJobsNewest(pageIndex: number, pageSize: number): Observable<JobListApiResponse> {
    return this.http.get<JobListApiResponse>(
      `${this.url}/jobs/newest/${pageIndex}/${pageSize}`
    );
  }

  getAmount(): Observable<JobCountApiResponse> {
    return this.http.get<JobCountApiResponse>(`${this.url}/jobs/count`);
  }

  getAddressCount(): Observable<JobAddressCountApiResponse> {
    return this.http.get<JobAddressCountApiResponse>(`${this.url}/jobs/addressCount`);
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload): Observable<JobListApiResponse> {
    return this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, filter);
  }
}
