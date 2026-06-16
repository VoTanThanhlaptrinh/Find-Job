import { HttpClient } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { map, Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';
import { ResumePreview } from '../../shared/models/jobs/resume-preview.model';
import { ResumeUrlDTO } from '../../shared/models/jobs/resume-url-dto.model';

import { FileMessage, SseMessagePayload } from '../../shared/models/sse/sse.model';
import { SseService } from './sse.service';

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
    private sseService = inject(SseService);
    readonly resumes$ = computed(() => this.resumes());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());

    private readonly sseProcessEvent = this.sseService.fromEvent<SseMessagePayload<Partial<FileMessage>>>(this.SSE_EVENT_NAME);

    readonly uploadingFile$ = computed<FileMessage | null>(() => {
        const local = this.localFileData();
        const sseEvent = this.sseProcessEvent();

        if (!local.name && !sseEvent) return null;

        const status = sseEvent?.status ?? local.status ?? 'pending';
        const id = sseEvent?.id ?? local.id ?? 0;
        const name = local.name ?? 'Unknown';

        return { id, status, name };
    });

    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService,
    ) {
        this.url = this.utilities.getURLDev();
        effect(() => {
            const event = this.sseProcessEvent();
            console.log('Received SSE Event:', event);
            if (event && event.status === 'analyzed') {
                this.resumes.update(list =>
                    list.map(r => r.id === event.id ? { ...r, isAnalyzed: true } : r)
                );
            }
        });

        // Auto-dismiss khi terminal state
        effect(() => {
            const file = this.uploadingFile$();
            if (file?.status === 'analyzed' || file?.status === 'failed') {
                setTimeout(() => {
                    this.localFileData.set({});
                    this.sseService.clearEvent(this.SSE_EVENT_NAME);
                }, 5000);
            }
        });
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

    postResume(file: File, enableAiAnalysis: boolean = false) {
        this.localFileData.set({ name: file.name, status: 'uploading', id: ++this.counter });

        const formData = new FormData();
        formData.append('file', file);
        formData.append('enableAiAnalysis', String(enableAiAnalysis));

        this.http.post<ApiResponse<ResumePreview>>(`${this.url}/user/resumes`, formData).subscribe({
            next: (response) => {
                const resumeId = response.data.id;

                this.localFileData.update(prev => ({ ...prev, id: resumeId }));
                this.resumes.update(resumes => [...resumes, {
                    id: resumeId,
                    fileName: file.name,
                    createDate: new Date().toISOString(),
                    isAnalyzed: false
                }]);

                // SSE đã kết nối sẵn từ lúc login, không cần connect ở đây nữa
            },
            error: (error) => {
                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                this.notificationService.error(error.error?.message || 'Lỗi khi tải CV');

                setTimeout(() => {
                    this.localFileData.set({});
                }, 3000);
            }
        });
    }

    analyzeResume(resumeId: number): void {
        this.localFileData.set({ name: '', status: 'analyzing', id: resumeId });

        this.http.post<ApiResponse<string>>(`${this.url}/user/resumes/${resumeId}/analyze`, {}).pipe(take(1)).subscribe({
            next: () => {
                // SSE sẽ cập nhật trạng thái real-time
            },
            error: (error) => {
                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                this.notificationService.error(error.error?.message || 'Lỗi khi phân tích CV');
            }
        });
    }
}
