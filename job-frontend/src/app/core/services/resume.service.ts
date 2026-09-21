import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { map, Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';
import { ResumePreview } from '../../shared/models/jobs/resume-preview.model';
import { ResumeUrlDTO } from '../../shared/models/jobs/resume-url-dto.model';
import { ResumeUploadInitiateRequest, ResumeUploadInitiateResponse } from '../../shared/models/jobs/resume-upload.model';
import { NO_AUTH } from '../interceptors/logger.interceptor';

import { FileMessage, SseMessagePayload } from '../../shared/models/sse/sse.model';
import { SseService } from './sse.service';
import { I18nService } from '../i18n/i18n.service';

export type ResumeContext = 'user' | 'hirer';

@Injectable({
    providedIn: 'root'
})
export class ResumeService {
    private url: string;
    private resumes = signal<ResumeReviewInput[]>([]);
    private analyzedResumes = signal<ResumeReviewInput[]>([]);
    private isLoadingResumes = signal<boolean>(false);
    private readonly SSE_EVENT_NAME = 'resume-process';
    readonly localFileData = signal<Partial<FileMessage> & { pendingResume?: ResumeReviewInput, isManualAnalyze?: boolean }>({});
    private counter = -10;
    private sseService = inject(SseService);
    private i18n = inject(I18nService);
    readonly resumes$ = computed(() => this.resumes());
    readonly analyzedResumes$ = computed(() => this.analyzedResumes());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());

    private readonly sseProcessEvent = this.sseService.fromEvent<SseMessagePayload<Partial<FileMessage>>>(this.SSE_EVENT_NAME);

    readonly uploadingFile$ = computed<(FileMessage & { isManualAnalyze?: boolean, executionTime?: number }) | null>(() => {
        const local = this.localFileData();
        const sseEvent = this.sseProcessEvent();

        if (local.id === undefined && !sseEvent) return null;

        if (local.status === 'uploading' || local.status === 'uploaded' || local.status === 'error') {
            return {
                id: local.id ?? 0,
                status: local.status,
                name: local.name || 'Unknown',
                isManualAnalyze: local.isManualAnalyze,
                executionTime: sseEvent?.executionTime
            };
        }

        const status = sseEvent?.status ?? local.status ?? 'pending';
        const id = sseEvent?.id ?? local.id ?? 0;
        const name = local.name || 'Unknown';
        const executionTime = sseEvent?.executionTime;

        return { id, status, name, isManualAnalyze: local.isManualAnalyze, executionTime };
    });

    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService,
    ) {
        this.url = this.utilities.getURLDev();
        effect(() => {
            const event = this.sseProcessEvent();
            if (event && event.status === 'analyzed') {
                this.resumes.update(list =>
                    list.map(r => r.id === event.id ? { ...r, isAnalyzed: true } : r)
                );
                const successMsg = this.i18n.translate('cvList.analyzedSuccess') || event.message || 'Phân tích CV thành công!';
                this.notificationService.success(successMsg);
            }
            if (event && event.status === 'failed') {
                const failMsg = event.message || this.i18n.translate('cvList.badge.failed') || 'Phân tích CV thất bại!';
                this.notificationService.error(failMsg);
            }
        });

        // Auto-dismiss khi terminal state
        effect(() => {
            const file = this.uploadingFile$();
            if (file?.status === 'analyzed' || file?.status === 'failed' || file?.status === 'uploaded' || file?.status === 'error') {
                const timeoutMs = (file.status === 'failed' || file.status === 'error') ? 2000 : (file.status === 'uploaded' ? 1000 : 5000);
                setTimeout(() => {
                    if (file.status === 'analyzed') {
                        this.resumes.update(list => list.map(r => r.id === file.id ? { ...r, isAnalyzed: true } : r));
                    }
                    if (file.status === 'error' || file.status === 'failed') {
                        this.resumes.update(list => list.filter(r => r.id !== file.id));
                    }
                    this.localFileData.set({});
                    this.sseService.clearEvent(this.SSE_EVENT_NAME);
                }, timeoutMs);
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

    getAnalyzedResumes() {
        this.isLoadingResumes.set(true);
        this.http.get<ApiResponse<ResumeReviewInput[]>>(`${this.url}/user/resumes/analyzed`).pipe(take(1)).subscribe({
            next: (response) => {
                this.analyzedResumes.set(response.data);
                this.isLoadingResumes.set(false);
            },
            error: () => {
                this.analyzedResumes.set([]);
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
        const intentKey = this.getUploadIntentKey(file);
        if (this.inFlightIntents.has(intentKey)) {
            // Ignore double-click for in-flight intent, avoiding duplicate optimistic cards
            return;
        }
        this.inFlightIntents.add(intentKey);

        this.sseService.clearEvent(this.SSE_EVENT_NAME);

        const tempId = --this.counter;
        const newResume: ResumeReviewInput = {
            id: tempId,
            fileName: file.name,
            createDate: new Date().toISOString(),
            isAnalyzed: false,
            isNewlyUploaded: true
        };

        this.resumes.update(resumes => [newResume, ...resumes]);
        this.localFileData.set({ name: file.name, status: 'uploading', id: tempId, isManualAnalyze: false });

        const idempotencyKey = this.getOrCreateIdempotencyKey(file);

        // 1. Initiate presigned upload with Idempotency-Key
        const initiatePayload: ResumeUploadInitiateRequest = {
            fileName: file.name,
            contentType: file.type || 'application/pdf',
            size: file.size
        };

        const headers = { 'Idempotency-Key': idempotencyKey };

        this.http.post<ApiResponse<ResumeUploadInitiateResponse>>(
            `${this.url}/user/resume-uploads/initiate`,
            initiatePayload,
            { headers }
        ).subscribe({
            next: (initResponse) => {
                const uploadData = initResponse.data;
                const uploadId = uploadData.uploadId;

                // 2. PUT file directly to S3/R2 presigned URL (bỏ qua tiêm JWT token)
                this.http.put(uploadData.uploadUrl, file, {
                    headers: uploadData.requiredHeaders || {},
                    context: new HttpContext().set(NO_AUTH, true)
                }).subscribe({
                    next: () => {
                        // 3. Complete upload
                        this.http.post<ApiResponse<{ id: number; fileName: string; createdAt: string; status: string }>>(
                            `${this.url}/user/resume-uploads/${uploadId}/complete`,
                            {}
                        ).subscribe({
                            next: (completeResponse) => {
                                this.inFlightIntents.delete(intentKey);
                                this.clearUploadIntent(file);
                                const resumeId = completeResponse.data.id;

                                this.resumes.update(list => list.map(r => r.id === tempId ? { ...r, id: resumeId } : r));

                                this.localFileData.update(prev => ({
                                    ...prev,
                                    id: resumeId,
                                    status: enableAiAnalysis ? 'analyzing' : 'uploaded'
                                }));

                                if (enableAiAnalysis) {
                                    this.analyzeResume(resumeId);
                                }
                            },
                            error: (completeErr) => {
                                this.inFlightIntents.delete(intentKey);
                                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                                this.notificationService.error(completeErr.error?.message || 'Lỗi khi xác nhận tải CV');
                            }
                        });
                    },
                    error: (storageErr) => {
                        this.inFlightIntents.delete(intentKey);
                        this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                        this.notificationService.error('Lỗi khi tải file lên kho lưu trữ');
                    }
                });
            },
            error: (initErr) => {
                this.inFlightIntents.delete(intentKey);
                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                this.notificationService.error(initErr.error?.message || 'Lỗi khi khởi tạo tải CV');
            }
        });
    }

    analyzeResume(resumeId: number): void {
        const resume = this.resumes().find(r => r.id === resumeId);
        const name = resume ? resume.fileName : 'Unknown';
        this.localFileData.set({ name, status: 'analyzing', id: resumeId, isManualAnalyze: true });

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

    private uploadIntents = new Map<string, string>();
    private inFlightIntents = new Set<string>();

    getUploadIntentKey(file: File): string {
        return `${file.name}_${file.size}_${file.lastModified}`;
    }

    isUploadInFlight(file: File): boolean {
        return this.inFlightIntents.has(this.getUploadIntentKey(file));
    }

    startNewUploadIntent(file: File): void {
        const fileKey = this.getUploadIntentKey(file);
        this.uploadIntents.delete(fileKey);
    }

    getOrCreateIdempotencyKey(file: File): string {
        const fileKey = this.getUploadIntentKey(file);
        let key = this.uploadIntents.get(fileKey);
        if (!key) {
            key = (typeof crypto !== 'undefined' && crypto.randomUUID) ? crypto.randomUUID() : this.generateUUID();
            this.uploadIntents.set(fileKey, key);
        }
        return key;
    }

    clearUploadIntent(file: File): void {
        const fileKey = this.getUploadIntentKey(file);
        this.uploadIntents.delete(fileKey);
    }

    private generateUUID(): string {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    uploadResumePresigned(file: File, enableAiAnalysis: boolean = false) {
        this.postResume(file, enableAiAnalysis);
    }
}
