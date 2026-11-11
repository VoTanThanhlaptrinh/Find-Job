import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, effect, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, switchMap, take, tap, throwError } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';
import { ResumePreview } from '../../shared/models/jobs/resume-preview.model';
import { ResumeUrlDTO } from '../../shared/models/jobs/resume-url-dto.model';
import {
    CompletedResumeUpload,
    ResumeUploadInitiateRequest,
    ResumeUploadInitiateResponse,
    StagedResumeUpload
} from '../../shared/models/jobs/resume-upload.model';
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
    readonly localFileData = signal<Partial<FileMessage>>({});
    private counter = -10;
    private sseService = inject(SseService);
    private i18n = inject(I18nService);
    readonly resumes$ = computed(() => this.resumes());
    readonly analyzedResumes$ = computed(() => this.analyzedResumes());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());

    private readonly sseProcessEvent = this.sseService.fromEvent<SseMessagePayload<Partial<FileMessage>>>(this.SSE_EVENT_NAME);

    readonly uploadingFile$ = computed<(FileMessage & { executionTime?: number; message?: string }) | null>(() => {
        const local = this.localFileData();
        const sseEvent = this.sseProcessEvent();

        if (local.id === undefined && !sseEvent) {
            return null;
        }

        const isUploadLocalState =
            local.status === 'uploading' || local.status === 'uploaded' || local.status === 'error';

        return {
            id: isUploadLocalState ? (local.id ?? 0) : (sseEvent?.id ?? local.id ?? 0),
            status: isUploadLocalState ? local.status! : (sseEvent?.status ?? local.status ?? 'pending'),
            name: local.name || 'Unknown',
            executionTime: sseEvent?.executionTime,
            message: sseEvent?.message
        };
    });

    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService,
    ) {
        this.url = this.utilities.getURLDev();
        const terminalDismissMs: Record<string, number> = {
            uploaded: 1000,
            error: 2000,
            failed: 2000,
            analyzed: 5000
        };

        effect((onCleanup) => {
            const file = this.uploadingFile$();
            if (!file) {
                return;
            }

            const dismissDelay = terminalDismissMs[file.status];
            if (dismissDelay === undefined) {
                return;
            }

            if (file.status === 'analyzed') {
                this.resumes.update(list => list.map(r => r.id === file.id ? { ...r, isAnalyzed: true } : r));
                const successMsg = this.i18n.translate('cvList.analyzedSuccess') || file.message || 'Phân tích CV thành công!';
                this.notificationService.success(successMsg);
            } else if (file.status === 'failed') {
                const failMsg = file.message || this.i18n.translate('cvList.badge.failed') || 'Phân tích CV thất bại!';
                this.notificationService.error(failMsg);
            }

            const timer = setTimeout(() => {
                if (file.status === 'error' && file.id < 0) {
                    this.resumes.update(list => list.filter(r => r.id !== file.id));
                }
                this.localFileData.set({});
                this.sseService.clearEvent(this.SSE_EVENT_NAME);
            }, dismissDelay);

            onCleanup(() => clearTimeout(timer));
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
        this.runTrackedUpload(
            file,
            tempId => this.stageCvToTemp(file).pipe(
                switchMap(staged => this.confirmStagedUpload(staged.uploadId, file, tempId))
            ),
            enableAiAnalysis,
            'Lỗi khi tải CV lên'
        );
    }

    completeStagedResume(uploadId: string, file: File, enableAiAnalysis: boolean = false): void {
        this.runTrackedUpload(
            file,
            tempId => this.confirmStagedUpload(uploadId, file, tempId),
            enableAiAnalysis,
            'Lỗi khi xác nhận tải CV'
        );
    }

    stageCvToTemp(file: File): Observable<StagedResumeUpload> {
        const idempotencyKey = this.getOrCreateIdempotencyKey(file);
        const contentType = this.resolveContentType(file);
        const initiatePayload: ResumeUploadInitiateRequest = {
            fileName: file.name,
            contentType,
            size: file.size
        };

        return this.http.post<ApiResponse<ResumeUploadInitiateResponse>>(
            `${this.url}/user/resume-uploads/initiate`,
            initiatePayload,
            { headers: { 'Idempotency-Key': idempotencyKey } }
        ).pipe(
            switchMap(initResponse => {
                const upload = initResponse.data;
                if (!upload?.uploadId || !upload.uploadUrl) {
                    this.clearUploadIntent(file);
                    return throwError(() => new Error('Không nhận được đường dẫn tải file lên vùng tạm'));
                }

                return this.http.put(upload.uploadUrl, file, {
                    headers: upload.requiredHeaders || { 'Content-Type': contentType },
                    context: new HttpContext().set(NO_AUTH, true)
                }).pipe(
                    map(() => ({
                        uploadId: upload.uploadId,
                        fileName: file.name,
                        expiresAt: upload.expiresAt
                    }))
                );
            }),
            catchError(err => {
                if (err?.status === 400 || err?.status === 404) {
                    this.clearUploadIntent(file);
                }
                return throwError(() => err);
            }),
            take(1)
        );
    }

    confirmStagedUpload(
        uploadId: string,
        file: File,
        replaceTempId?: number
    ): Observable<CompletedResumeUpload> {
        return this.http.post<ApiResponse<CompletedResumeUpload>>(
            `${this.url}/user/resume-uploads/${encodeURIComponent(uploadId)}/complete`,
            {}
        ).pipe(
            map(response => response.data),
            tap(resume => {
                this.clearUploadIntent(file);
                this.upsertResumeCard(resume, replaceTempId);
            }),
            catchError(err => {
                if (err?.status === 400 || err?.status === 404) {
                    this.clearUploadIntent(file);
                }
                return throwError(() => err);
            }),
            take(1)
        );
    }

    private runTrackedUpload(
        file: File,
        uploadFactory: (tempId: number) => Observable<CompletedResumeUpload>,
        enableAiAnalysis: boolean,
        fallbackErrorMessage: string
    ): void {
        const intentKey = this.getUploadIntentKey(file);
        if (this.inFlightIntents.has(intentKey)) {
            return;
        }
        this.inFlightIntents.add(intentKey);
        this.sseService.clearEvent(this.SSE_EVENT_NAME);

        const tempId = --this.counter;
        const optimisticResume: ResumeReviewInput = {
            id: tempId,
            fileName: file.name,
            createDate: new Date().toISOString(),
            isAnalyzed: false,
            isNewlyUploaded: true
        };

        this.resumes.update(resumes => [optimisticResume, ...resumes]);
        this.localFileData.set({ name: file.name, status: 'uploading', id: tempId });

        uploadFactory(tempId).pipe(
            finalize(() => this.inFlightIntents.delete(intentKey))
        ).subscribe({
            next: (resume) => {
                this.localFileData.update(prev => ({
                    ...prev,
                    id: resume.id,
                    status: enableAiAnalysis ? 'analyzing' : 'uploaded'
                }));

                if (enableAiAnalysis) {
                    this.analyzeResume(resume.id);
                }
            },
            error: (err) => {
                this.localFileData.update(prev => ({ ...prev, status: 'error' }));
                this.notificationService.error(err?.error?.message || fallbackErrorMessage);
            }
        });
    }

    private upsertResumeCard(resume: CompletedResumeUpload, replaceTempId?: number): void {
        const entry: ResumeReviewInput = {
            id: resume.id,
            fileName: resume.fileName,
            createDate: resume.createDate ?? resume.createdAt ?? new Date().toISOString(),
            isAnalyzed: false
        };

        if (replaceTempId !== undefined) {
            this.resumes.update(list =>
                list
                    .filter(item => item.id !== resume.id)
                    .map(item => item.id === replaceTempId ? { ...item, ...entry } : item)
            );
            return;
        }

        this.resumes.update(list => [entry, ...list.filter(item => item.id !== resume.id)]);
    }

    analyzeResume(resumeId: number): void {
        const resume = this.resumes().find(r => r.id === resumeId);
        const name = resume ? resume.fileName : 'Unknown';
        this.localFileData.set({ name, status: 'analyzing', id: resumeId });

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

    private resolveContentType(file: File): string {
        if (file.type) {
            return file.type;
        }
        const extension = file.name.split('.').pop()?.toLowerCase();
        if (extension === 'doc') return 'application/msword';
        if (extension === 'docx') return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        return 'application/pdf';
    }
}
