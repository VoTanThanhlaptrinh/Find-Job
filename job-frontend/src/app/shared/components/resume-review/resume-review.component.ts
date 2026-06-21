import { Component, ElementRef, HostListener, Input, inject, computed, signal, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResumeContext, ResumeService } from '../../../core/services/resume.service';
import { ResumeReviewInput } from '../../models/jobs/resume-review-input.model';
import { NotifyMessageService } from '../../../core/services/notify-message.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-resume-review',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './resume-review.component.html',
  styleUrl: './resume-review.component.css'
})
export class ResumeReviewComponent implements OnDestroy {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly resumeService = inject(ResumeService);
  private readonly notifyService = inject(NotifyMessageService);
  private readonly i18n = inject(I18nService);

  @Input({ required: true }) resume!: ResumeReviewInput;
  @Input() context: ResumeContext = 'user';
  @Input() allowDelete = true;
  @Input() isOfficial = false;

  isMenuOpen = false;
  isDeleting = false;
  isLoadingView = false;
  isLoadingDownload = false;
  
  uploadProgress = signal(0);
  private progressInterval: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      if (this.isUploading() || this.isParsing() || this.isVectorizing()) {
        this.startFakeProgress();
      } else {
        this.stopFakeProgress();
        if (this.isUploaded() || this.resume.isAnalyzed) {
          this.uploadProgress.set(100);
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.stopFakeProgress();
  }

  private startFakeProgress(): void {
    if (this.progressInterval) return;
    this.uploadProgress.set(0);
    this.progressInterval = setInterval(() => {
      this.uploadProgress.update(prev => {
        if (this.isParsing() && prev >= 70) return 70; // Cap at 70% during parsing
        if (this.isVectorizing()) {
           if (prev < 70) return 70; // Jump to 70
           if (prev >= 90) return 90; // Cap at 90% during vectorizing
        }

        const increment = prev < 50 ? 10 : prev < 80 ? 5 : 2;
        if (prev + increment >= 95) {
          return 95;
        }
        return prev + increment;
      });
    }, 300);
  }

  private stopFakeProgress(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  readonly isUploading = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && file.status === 'uploading';
  });

  readonly isUploaded = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && file.status === 'uploaded';
  });

  readonly isAnalyzing = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && (file.status === 'analyzing' || file.status === 'parsing' || file.status === 'vectorizing');
  });

  readonly isParsing = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && file.status === 'parsing';
  });

  readonly isVectorizing = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && file.status === 'vectorizing';
  });

  readonly isFailed = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file !== null && file.id === this.resume.id && (file.status === 'failed' || file.status === 'error');
  });

  readonly executionTime = computed(() => {
    const file = this.resumeService.uploadingFile$();
    if (file !== null && file.id === this.resume.id && file.executionTime !== undefined) {
      return file.executionTime;
    }
    return null;
  });

  onAnalyzeResume(): void {
    this.resumeService.analyzeResume(this.resume.id);
  }

  readonly officialLabel = computed(() => this.i18n.translate('cvList.officialTag'));
  readonly downloadLabel = computed(() => this.i18n.translate('cvList.download'));
  readonly deleteLabel = computed(() => this.i18n.translate('cvList.delete'));
  readonly deletingLabel = computed(() => this.i18n.translate('cvList.deleting'));
  readonly viewLabel = computed(() => this.i18n.translate('cvList.view'));

  // Menu is controlled via direct click bindings in the template

  closeMenu(): void {
    this.isMenuOpen = false;
  }

  onViewResume(): void {
    if (this.isLoadingView) {
      return;
    }

    this.isLoadingView = true;
    this.resumeService.getResumeViewUrl(this.resume.id, this.context).subscribe({
      next: (url) => {
        window.open(url, '_blank', 'noopener,noreferrer');
        this.isLoadingView = false;
      },
      error: () => {
        this.notifyService.error('Không thể xem CV. Vui lòng thử lại.');
        this.isLoadingView = false;
      }
    });
  }

  onDownloadResume(): void {
    if (this.isLoadingDownload) {
      return;
    }

    this.isLoadingDownload = true;
    this.resumeService.getResumeDownloadUrl(this.resume.id, this.context).subscribe({
      next: (url) => {
        const link = document.createElement('a');
        link.href = url;
        link.download = this.resume.fileName;
        link.target = '_blank';
        link.rel = 'noopener noreferrer';
        link.click();
        this.isLoadingDownload = false;
      },
      error: () => {
        this.notifyService.error('Không thể tải CV. Vui lòng thử lại.');
        this.isLoadingDownload = false;
      }
    });
  }

  onDeleteResume(): void {
    if (this.isDeleting) {
      return;
    }

    this.isDeleting = true;
    this.resumeService.deleteResume(this.resume.id).subscribe({
      next: () => {
        this.isDeleting = false;
        this.closeMenu();
      },
      error: () => {
        this.isDeleting = false;
      }
    });
  }

  get fileIconType(): 'pdf' | 'doc' | 'unknown' {
    const lowerCaseName = this.resume.fileName.toLowerCase();
    if (lowerCaseName.endsWith('.pdf')) return 'pdf';
    if (lowerCaseName.endsWith('.doc') || lowerCaseName.endsWith('.docx')) return 'doc';
    return 'unknown';
  }

  get createDateLabel(): string {
    if (!this.resume.createDate) {
      return '--';
    }

    const date = new Date(this.resume.createDate);
    if (Number.isNaN(date.getTime())) {
      return this.resume.createDate;
    }

    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    }).format(date);
  }
}
