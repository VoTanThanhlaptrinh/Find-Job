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
      if (this.isBusy()) {
        this.startFakeProgress();
        return;
      }
      this.stopFakeProgress();
      const status = this.processStatus();
      if (status === 'uploaded' || status === 'analyzed') {
        this.uploadProgress.set(100);
      }
    });
  }

  ngOnDestroy(): void {
    this.stopFakeProgress();
  }

  private startFakeProgress(): void {
    if (this.progressInterval) return;
    const progressCap: Record<string, number> = {
      analyzing: 40,
      parsing: 70,
      vectorizing: 90,
      uploading: 95
    };
    this.uploadProgress.set(0);
    this.progressInterval = setInterval(() => {
      const status = this.processStatus();
      const cap = progressCap[status] ?? 95;
      this.uploadProgress.update(prev => {
        if (status === 'vectorizing' && prev < 70) {
          return 70;
        }
        if (prev >= cap) {
          return cap;
        }
        const increment = prev < 50 ? 10 : prev < 80 ? 5 : 2;
        return Math.min(prev + increment, cap);
      });
    }, 300);
  }

  private stopFakeProgress(): void {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
  }

  private readonly activeProcess = computed(() => {
    const file = this.resumeService.uploadingFile$();
    return file?.id === this.resume.id ? file : null;
  });

  readonly processStatus = computed(() => {
    const activeStatus = this.activeProcess()?.status;
    if (activeStatus) {
      return activeStatus;
    }
    return this.resume?.isAnalyzed ? 'analyzed' : 'idle';
  });

  readonly isBusy = computed(() => {
    const status = this.processStatus();
    return status === 'uploading' || status === 'analyzing' || status === 'parsing' || status === 'vectorizing';
  });

  readonly isFailed = computed(() => {
    const status = this.processStatus();
    return status === 'failed' || status === 'error';
  });

  readonly executionTime = computed(() => this.activeProcess()?.executionTime ?? null);

  onAnalyzeResume(): void {
    this.resumeService.analyzeResume(this.resume.id);
  }

  readonly officialLabel = computed(() => this.i18n.translate('cvList.officialTag'));
  readonly downloadLabel = computed(() => this.i18n.translate('cvList.download'));
  readonly deleteLabel = computed(() => this.i18n.translate('cvList.delete'));
  readonly deletingLabel = computed(() => this.i18n.translate('cvList.deleting'));
  readonly viewLabel = computed(() => this.i18n.translate('cvList.view'));

  // Menu is controlled via direct click bindings in the template
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.isMenuOpen && !this.elementRef.nativeElement.contains(event.target as Node)) {
      this.closeMenu();
    }
  }

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
        this.notifyService.error(this.i18n.translate('cvList.errors.viewFailed'));
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
        this.notifyService.error(this.i18n.translate('cvList.errors.downloadFailed'));
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
