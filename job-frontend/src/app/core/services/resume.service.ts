import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';

@Injectable({
    providedIn: 'root'
})
export class ResumeService {
    private url: string;
    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService
    ) {
        this.url = this.utilities.getURLDev();
    }

    getUserResumes(): Observable<ApiResponse<ResumeReviewInput[]>> {
        return this.http.get<ApiResponse<ResumeReviewInput[]>>(`${this.url}/user/resumes`);
    }

    getResumeResourceUrl(resumeId: number, fileName: string, mode: 'inline' | 'attachment'): string {
        const encodedFileName = encodeURIComponent(fileName);
        const encodedResumeId = encodeURIComponent(String(resumeId));
        return `${this.url}/${encodedResumeId}?mode=${mode}&fileName=${encodedFileName}`;
    }

    deleteResume(resumeId: number): Observable<unknown> {
        const encodedResumeId = encodeURIComponent(String(resumeId));
        return this.http.delete(`${this.url}/user/resumes/${encodedResumeId}`);
    }
}
