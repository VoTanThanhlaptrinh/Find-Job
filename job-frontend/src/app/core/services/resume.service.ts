import { HttpClient } from '@angular/common/http';
import { computed, Injectable, NgZone, signal } from '@angular/core';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ResumeReviewInput } from '../../shared/models/jobs/resume-review-input.model';
import { ApiResponse } from '../../shared/models/api-response.model';
import { NotifyMessageService } from './notify-message.service';
import { TokenService } from './token.service';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { ResumePreview } from '../../shared/models/jobs/resume-preview.model';

export interface UploadingFileState {
    fileName: string;
    status: 'uploading' | 'uploaded' | 'analyzing' | 'analyzed' | 'error';
}

@Injectable({
    providedIn: 'root'
})
export class ResumeService {
    private url: string;
    private resumes = signal<ResumeReviewInput[]>([]);
    private uploadingFile = signal<UploadingFileState | null>(null);
    private isLoadingResumes = signal<boolean>(false);
    private activeEventSource: any = null;
    private analysisTimeoutId: ReturnType<typeof setTimeout> | null = null;
    readonly resumes$ = computed(() => this.resumes());
    readonly uploadingFile$ = computed(() => this.uploadingFile());
    readonly isLoadingResumes$ = computed(() => this.isLoadingResumes());
    constructor(
        private http: HttpClient,
        private utilities: UtilitiesService,
        private notificationService: NotifyMessageService,
        private ngZone: NgZone,
        private tokenService: TokenService
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
        this.resumes.update(resumes => resumes.filter(resume => resume.id !== resumeId));
        return this.http.delete(`${this.url}/user/resumes/${encodedResumeId}`).pipe(take(1));
    }
    postResume(file: File) {
        this.cleanupSSE();
        this.uploadingFile.set({ fileName: file.name, status: 'uploading' });
        const formData = new FormData();
        formData.append('file', file);
        this.http.post<ApiResponse<ResumePreview>>(`${this.url}/user/resumes`, formData).pipe(take(1)).subscribe({
            next: (response) => {
                const resumeId = response.data.id;
                this.uploadingFile.set({ fileName: file.name, status: 'uploaded' });
                setTimeout(() => {
                    this.listenForAnalysis(file.name, Number(resumeId));
                }, 1500);
            },
            error: (error) => {
                this.uploadingFile.set({ fileName: file.name, status: 'error' });
                this.notificationService.error(error.error.message);
                setTimeout(() => this.uploadingFile.set(null), 3000);
            }
        });
    }

    private listenForAnalysis(fileName: string, resumeId: number): void {
        this.uploadingFile.set({ fileName, status: 'analyzing' });

        const token = this.tokenService.getToken();
        const eventSource = new EventSourcePolyfill(`${this.url}/notifications/${resumeId}`, {
            headers: {
                Authorization: `Bearer ${token}`,
                'Accept': 'text/event-stream'
            }
        });
        this.activeEventSource = eventSource;

        this.analysisTimeoutId = setTimeout(() => {
            this.ngZone.run(() => {
                this.cleanupSSE();
                this.uploadingFile.set({ fileName, status: 'error' });
                this.notificationService.error('Phân tích CV quá thời gian. Vui lòng thử lại.');
                setTimeout(() => this.uploadingFile.set(null), 3000);
            });
        }, 60000);

        const handleMessage = (event: any) => {
            this.ngZone.run(() => {
                this.cleanupSSE();
                
                let isSuccess = true;
                let messageStr = 'Phân tích CV hoàn tất.';

                if (event && event.data) {
                    try {
                        const response = JSON.parse(event.data);
                        if (response.status !== undefined && response.status !== 200) {
                            isSuccess = false;
                        }
                        if (response.message) {
                            messageStr = response.message;
                        }
                    } catch (e) {
                         // Fallback nếu event.data thuần là string
                         if (typeof event.data === 'string' && event.data.trim().length > 0) {
                             if (event.data.toLowerCase().includes('error') || event.data.toLowerCase().includes('fail') || event.data.toLowerCase().includes('lỗi')) {
                                 isSuccess = false;
                             }
                             // Gán messageStr bằng string từ server nếu không phải dạng status success/error đặc biệt
                             messageStr = event.data;
                         }
                    }
                }

                if (isSuccess) {
                    this.uploadingFile.set({ fileName, status: 'analyzed' });
                    this.notificationService.success(messageStr);
                    setTimeout(() => {
                        this.getUserResumes();
                        this.uploadingFile.set(null);
                    }, 2000);
                } else {
                    this.uploadingFile.set({ fileName, status: 'error' });
                    this.notificationService.error('Phân tích CV lỗi: ' + messageStr);
                    setTimeout(() => this.uploadingFile.set(null), 3000);
                }
            });
        };

        // Lắng nghe tin nhắn cơ bản
        eventSource.onmessage = handleMessage;

        // Lắng nghe sự kiện "notification" từ Spring backend gửi bằng .name("notification")
        eventSource.addEventListener('notification', handleMessage);

        // Lắng nghe sự kiện "connect" từ Spring backend gửi bằng .name("connect")
        eventSource.addEventListener('connect', (event: any) => {
            console.log('Đã kết nối SSE:', event.data);
            // Vẫn giữ kết nối để chờ thông báo phân tích
        });

        eventSource.onerror = (error: any) => {
            this.ngZone.run(() => {
                this.cleanupSSE();
                this.uploadingFile.set({ fileName, status: 'error' });
                
                let errorMsg = 'Lỗi kết nối khi phân tích CV. Vui lòng thử lại.';
                if (error && error.status) {
                    if (error.status === 401) {
                        errorMsg = 'Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.';
                    } else if (error.status === 403) {
                        errorMsg = 'Bạn không có quyền thao tác trên CV này.';
                    }
                }
                
                this.notificationService.error(errorMsg);
                setTimeout(() => this.uploadingFile.set(null), 3000);
            });
        };
    }

    private cleanupSSE(): void {
        if (this.analysisTimeoutId) {
            clearTimeout(this.analysisTimeoutId);
            this.analysisTimeoutId = null;
        }
        if (this.activeEventSource) {
            this.activeEventSource.close();
            this.activeEventSource = null;
        }
    }
}
