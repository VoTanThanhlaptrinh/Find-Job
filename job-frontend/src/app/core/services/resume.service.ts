import { HttpClient } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { map, Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';
import { TokenService } from './token.service';
import { ResumePreview } from '../../shared/models/jobs/resume-preview.model';
import { ResumeUrlDTO } from '../../shared/models/jobs/resume-url-dto.model';
import { SseService } from './sse.service';
import { FileMessage, SseMessagePayload } from '../../shared/models/sse/sse.model';

export type ResumeContext = 'user' | 'hirer';

@Injectable({
    providedIn: 'root'
})
export class ResumeService {
    private url: string;
    private resumes = signal<ResumeReviewInput[]>([]);
    private isLoadingResumes = signal<boolean>(false);
    private readonly SSE_EVENT_NAME = 'resume-process';
    readonly localFileData = signal<Partial<FileMessage>>({});
    private counter = -10;

    readonly resumes$ = computed(() => this.resumes());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());

    readonly uploadingFile$ = computed<FileMessage | null>(() => {
        const local = this.localFileData();
        const sseEvent = this.sseService.latestEvents()[this.SSE_EVENT_NAME] as SseMessagePayload<Partial<FileMessage>> | undefined;
        if (!local.name && !sseEvent) return null;

        return {
            id: sseEvent?.data?.id ?? sseEvent?.id ?? local.id ?? 0,
            status: sseEvent?.data?.status ?? sseEvent?.status ?? local.status ?? 'pending',
            name: sseEvent?.data?.name ?? local.name ?? 'Unknown_Resume'
        };
    });
    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService,
        private tokenService: TokenService,
        private sseService: SseService,
    ) {
        this.url = this.utilities.getURLDev();
        this.sseService.latestEvents()['resume-progress']
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

    getResumeViewUrl(resumeId: number, context: ResumeContext = 'user'): Observable<string> {
        const prefix = context === 'hirer' ? 'hirer' : 'user';
        return this.http.get<ApiResponse<ResumeUrlDTO>>(
            `${this.url}/${prefix}/resumes/${encodeURIComponent(resumeId)}/view`
        ).pipe(
            take(1),
            map(response => response.data.url)
        );
    }

    getResumeDownloadUrl(resumeId: number, context: ResumeContext = 'user'): Observable<string> {
        const prefix = context === 'hirer' ? 'hirer' : 'user';
        return this.http.get<ApiResponse<ResumeUrlDTO>>(
            `${this.url}/${prefix}/resumes/${encodeURIComponent(resumeId)}/download`
        ).pipe(
            take(1),
            map(response => response.data.url)
        );
    }

    deleteResume(resumeId: number): Observable<unknown> {
        const encodedResumeId = encodeURIComponent(String(resumeId));
        this.resumes.update(resumes => resumes.filter(resume => resume.id !== resumeId));
        return this.http.delete(`${this.url}/user/resumes/${encodedResumeId}`).pipe(take(1));
    }
    postResume(file: File) {
        this.localFileData.set({ name: file.name, status: 'uploading', id: ++this.counter });
        const formData = new FormData();
        formData.append('file', file);
        this.http.post<ApiResponse<ResumePreview>>(`${this.url}/user/resumes`, formData).subscribe({
            next: (response) => {
                const resumeId = response.data.id;
                this.localFileData.update(prev => ({ ...prev, status: 'uploaded' }));
                this.sseService.connect(`${this.url}/api/notifications/${resumeId}`, [this.SSE_EVENT_NAME], { withCredentials: true });
            },
            error: (error) => {
                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                this.notificationService.error(error.error.message);
                setTimeout(() => {
                    this.localFileData.set({});
                    this.sseService.disconnect();
                }, 3000);
            }
        });
    }

    // Thiết lập Headers
    private getSseHeaders(): Record<string, string> {
        const headers: Record<string, string> = {
            Accept: 'text/event-stream',
            'Cache-Control': 'no-cache'
        };
        const token = this.tokenService.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    }
}
