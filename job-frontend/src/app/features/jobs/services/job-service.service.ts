import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
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
    return this.http.get<JobDetailApiResponse>(`${this.url}/jobs/${id}`);
  }

  checkApplyJob(id: number): Observable<JobExistsApiResponse> {
    return this.http.get<JobExistsApiResponse>(
      `${this.url}/user/applications/jobs/${id}/status`,
      { withCredentials: true }
    );
  }

  submitApplyCvExisting(
    payload: ApplyCvWithExistingRequest
  ): Observable<ApplyCvWithExistingResponse> {
    return this.http.post<ApplyCvWithExistingResponse>(
      `${this.url}/user/applications/submit-existing`,
      payload,
      { withCredentials: true }
    );
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
    );
  }

  doPostJob(form: FormData): Observable<JobSubmitApiResponse> {
    return this.http.post<JobSubmitApiResponse>(`${this.url}/hirer/jobs`, form, {
      withCredentials: true,
    });
  }

  getHirerJobPost(pageIndex: number, pageSize: number): Observable<HirerJobListApiResponse> {
    return this.http.get<HirerJobListApiResponse>(
      `${this.url}/hirer/jobs/posted/${pageIndex}/${pageSize}`,
      { withCredentials: true }
    );
  }

  countHirerJobPost(): Observable<HirerJobCountApiResponse> {
    return this.http.get<HirerJobCountApiResponse>(
      `${this.url}/hirer/jobs/posted/count`,
      { withCredentials: true }
    );
  }

  filterWithAddressTimeSalary(filter: JobFilterPayload): Observable<JobListApiResponse> {
    return this.http.post<JobListApiResponse>(`${this.url}/jobs/filter`, filter);
  }
}
