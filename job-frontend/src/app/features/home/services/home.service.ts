import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { HomeInitApiResponse } from '../../../shared/models/jobs/job-api-response.model';
import { JobCardModel } from '../../../shared/models/jobs/job-card.model';
import { map } from 'jquery';
import { ApiResponse } from '../../../shared/models/api-response.model';

@Injectable({
  providedIn: 'root',
})
export class HomeService {
  private url: string;
  jobData = signal<JobCardModel[]>([]);
  jobPosts = computed(() => this.jobData());
  isLoading = signal<boolean>(false);
  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  getData(): void {
    this.isLoading.set(true);
    this.http.get<ApiResponse<JobCardModel[]>>(`${this.url}/home/init`).pipe(take(1)).subscribe({
      next: (response) => {
        this.jobData.set(response.data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error fetching data:', error);
        this.isLoading.set(false);
      },
    });
  }
}
