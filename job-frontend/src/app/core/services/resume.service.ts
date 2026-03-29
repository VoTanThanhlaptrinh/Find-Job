import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';

export interface UploadingFileState {
    fileName: string;
    status: 'uploading' | 'success' | 'error';
}

@Injectable({
    providedIn: 'root'
})
export class ResumeService {
    private url: string;
    private resumes = signal<ResumeReviewInput[]>([]);
    private uploadingFile = signal<UploadingFileState | null>(null);
    private isLoadingResumes = signal<boolean>(false);
    readonly resumes$ = computed(() => this.resumes());
    readonly uploadingFile$ = computed(() => this.uploadingFile());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());
    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService
    ) {
        this.url = this.utilities.getURLDev();
    }

    getUserResumes() {
        this.isLoadingResumes.set(true);
        this.http.get<ApiResponse<ResumeReviewInput[]>>(`${this.url}/user/resumes`).pipe(take(1)).subscribe({
            next: (response) => {
                this.resumes.set(response.data);
                this.isLoadingResumes.set(false);
            },
            error: () => {
                this.resumes.set([]);
                this.isLoadingResumes.set(false);
            }
        });
    }

    getResumeResourceUrl(resumeId: number, fileName: string, mode: 'inline' | 'attachment'): string {
        const encodedFileName = encodeURIComponent(fileName);
        const encodedResumeId = encodeURIComponent(String(resumeId));
        return `${this.url}/${encodedResumeId}?mode=${mode}&fileName=${encodedFileName}`;
    }

    deleteResume(resumeId: number): Observable<unknown> {
        const encodedResumeId = encodeURIComponent(String(resumeId));
        return this.http.delete(`${this.url}/user/resumes/${encodedResumeId}`).pipe(take(1));
    }
    postResume(file: File) {
        this.uploadingFile.set({ fileName: file.name, status: 'uploading' });
        const formData = new FormData();
        formData.append('file', file);
        this.http.post<ApiResponse<ResumeReviewInput>>(`${this.url}/user/resumes`, formData).pipe(take(1)).subscribe({
            next: (response) => {
                this.uploadingFile.set({ fileName: file.name, status: 'success' });
                setTimeout(() => {
                    this.resumes.set([response.data, ...this.resumes()]);
                    this.uploadingFile.set(null);
                }, 3000);
            },
            error: (error) => {
                this.uploadingFile.set({ fileName: file.name, status: 'error' });
                this.notificationService.error(error.error.message);
                setTimeout(() => this.uploadingFile.set(null), 3000);
            }
        });
    }
}
