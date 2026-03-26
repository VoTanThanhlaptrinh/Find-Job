import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';
import {
  ApplyCvWithExistingRequest,
  ApplyCvWithExistingResponse,
  ApplyCvWithUploadRequest,
  ApplyCvWithUploadResponse,
} from '../../../shared/models/jobs/apply-cv.model';
import {
  HirerJobCountApiResponse,
  HirerJobListApiResponse,
  JobDetailApiResponse,
  JobExistsApiResponse,
  JobFilterPayload,
  JobListApiResponse,
  JobSubmitApiResponse,
} from '../../../shared/models/jobs/job-api-response.model';

@Injectable({
  providedIn: 'root',
})
export class JobServiceService {
  private url: string;

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  getDetailJob(id: string): Observable<JobDetailApiResponse> {
    return this.http.get<JobDetailApiResponse>(`${this.url}/jobs/${id}`).pipe(take(1));
  }

  checkApplyJob(id: number): Observable<JobExistsApiResponse> {
    return this.http.get<JobExistsApiResponse>(
      `${this.url}/user/applications/jobs/${id}/status`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  submitApplyCvExisting(
    payload: ApplyCvWithExistingRequest
  ): Observable<ApplyCvWithExistingResponse> {
    return this.http.post<ApplyCvWithExistingResponse>(
      `${this.url}/user/applications/submit-existing`,
      payload,
      { withCredentials: true }
    ).pipe(take(1));
  }

  submitApplyCvUpload(
    payload: ApplyCvWithUploadRequest
  ): Observable<ApplyCvWithUploadResponse> {
    const formData = new FormData();
    formData.append('jobId', payload.jobId.toString());
    formData.append('cvFile', payload.cvFile);
    formData.append('email', payload.email);
    formData.append('coverLetter', payload.coverLetter);

    return this.http.post<ApplyCvWithUploadResponse>(
      `${this.url}/user/applications/submit-upload`,
      formData,
      { withCredentials: true }
    ).pipe(take(1));
  }

  doPostJob(form: FormData): Observable<JobSubmitApiResponse> {
    return this.http.post<JobSubmitApiResponse>(`${this.url}/hirer/jobs`, form, {
      withCredentials: true,
    }).pipe(take(1));
  }

  getHirerJobPost(pageIndex: number, pageSize: number): Observable<HirerJobListApiResponse> {
    return this.http.get<HirerJobListApiResponse>(
      `${this.url}/hirer/jobs/posted/${pageIndex}/${pageSize}`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  countHirerJobPost(): Observable<HirerJobCountApiResponse> {
    return this.http.get<HirerJobCountApiResponse>(
      `${this.url}/hirer/jobs/posted/count`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload): Observable<JobListApiResponse> {
    return this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, filter).pipe(take(1));
  }
}
