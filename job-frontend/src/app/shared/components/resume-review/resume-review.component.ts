import { Component, ElementRef, HostListener, Input, inject } from '@angular/core';
import { ResumeContext, ResumeService } from '../../../core/services/resume.service';
import { ResumeReviewInput } from '../../models/jobs/resume-review-input.model';
import { NotifyMessageService } from '../../../core/services/notify-message.service';

@Component({
  selector: 'app-resume-review',
  standalone: true,
  templateUrl: './resume-review.component.html',
  styleUrl: './resume-review.component.css'
})
export class ResumeReviewComponent {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly resumeService = inject(ResumeService);
  private readonly notifyService = inject(NotifyMessageService);

  @Input({ required: true }) resume!: ResumeReviewInput;
  @Input() context: ResumeContext = 'user';
  @Input() allowDelete = true;
  isMenuOpen = false;
  isDeleting = false;
  isLoadingView = false;
  isLoadingDownload = false;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.isMenuOpen) {
      return;
    }

    const clickedElement = event.target as Node | null;
    const isClickInside = !!clickedElement && this.elementRef.nativeElement.contains(clickedElement);

    if (!isClickInside) {
      this.closeMenu();
    }
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
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

  get fileIconPath(): string {
    const lowerCaseName = this.resume.fileName.toLowerCase();
    if (lowerCaseName.endsWith('.doc') || lowerCaseName.endsWith('.docx')) {
      return 'assets/images/icons/file-doc.svg';
    }
    return 'assets/images/icons/file-pdf.svg';
  }

  get createDateLabel(): string {
    if (!this.resume.createDate) {
      return 'Ngày tải lên: --';
    }

    const date = new Date(this.resume.createDate);
    if (Number.isNaN(date.getTime())) {
      return `Ngày tải lên: ${this.resume.createDate}`;
    }

    const datePart = new Intl.DateTimeFormat('vi-VN').format(date);
    const timePart = new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    }).format(date);

    return `Ngày tải lên: ${datePart} | ${timePart}`;
  }
}
