import { Component, ElementRef, HostListener, Input, inject } from '@angular/core';
import { ResumeService } from '../../../core/services/resume.service';
import { ResumeReviewInput } from '../../models/jobs/resume-review-input.model';

@Component({
  selector: 'app-resume-review',
  standalone: true,
  templateUrl: './resume-review.component.html',
  styleUrl: './resume-review.component.css'
})
export class ResumeReviewComponent {
  private readonly elementRef = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly resumeService = inject(ResumeService);

  @Input({ required: true }) resume!: ResumeReviewInput;
  isMenuOpen = false;
  isDeleting = false;

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
    this.closeMenu();
    const url = this.resumeService.getResumeResourceUrl(this.resume.id, this.resume.fileName, 'inline');
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  onDownloadResume(): void {
    this.closeMenu();
    const link = document.createElement('a');
    link.href = this.resumeService.getResumeResourceUrl(this.resume.id, this.resume.fileName, 'attachment');
    link.download = this.resume.fileName;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.click();
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
