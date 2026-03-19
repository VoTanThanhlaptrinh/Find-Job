import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { HomeInitApiResponse } from '../../../shared/models/jobs/job-api-response.model';

@Injectable({
  providedIn: 'root',
})
export class HomeService {
  private url: string;

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  getData(): Observable<HomeInitApiResponse> {
    return this.http.get<HomeInitApiResponse>(`${this.url}/home/init`).pipe(take(1));
  }
}
