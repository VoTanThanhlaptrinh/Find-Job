import { Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ResumeService } from '../../../core/services/resume.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-cv-upload-modal',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './cv-upload-modal.component.html',
  styleUrl: './cv-upload-modal.component.css'
})
export class CvUploadModalComponent implements OnChanges, OnDestroy {
  private readonly i18n = inject(I18nService);
  private readonly resumeService = inject(ResumeService);

  @ViewChild('cvUploadInput') cvUploadInput?: ElementRef<HTMLInputElement>;
  @Input() isOpen = false;
  @Output() closeRequested = new EventEmitter<void>();
  @Output() submitRequested = new EventEmitter<{ file: File; uploadId: string; enableAiAnalysis: boolean }>();

  selectedFile: File | null = null;
  stagedUploadId: string | null = null;
  isUploadingToTemp = false;
  errorMessage = '';
  enableAiAnalysis = false;
  isSubmitting = false;
  private uploadSubscription?: Subscription;
  readonly maxFileSize = 10 * 1024 * 1024;
  readonly acceptedExtensions = '.pdf,.doc,.docx';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && !this.isOpen) {
      this.resetSelection();
    }
  }

  ngOnDestroy(): void {
    this.uploadSubscription?.unsubscribe();
  }

  onBackdropClick(): void {
    this.closeRequested.emit();
  }

  onModalContentClick(event: MouseEvent): void {
    event.stopPropagation();
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    this.setSelectedFile(file);
  }

  onSubmit(): void {
    if (this.isSubmitting) {
      return;
    }
    if (!this.selectedFile) {
      this.errorMessage = this.i18n.translate('cvUploadModal.selectFileHint');
      return;
    }
    if (this.isUploadingToTemp) {
      this.errorMessage = this.i18n.translate('cvUploadModal.tempUploadInProgress');
      return;
    }
    if (!this.stagedUploadId) {
      this.errorMessage = this.i18n.translate('cvUploadModal.tempUploadFailed');
      return;
    }

    this.isSubmitting = true;
    this.submitRequested.emit({
      file: this.selectedFile,
      uploadId: this.stagedUploadId,
      enableAiAnalysis: this.enableAiAnalysis
    });
  }

  onClose(event?: MouseEvent): void {
    event?.stopPropagation();
    this.closeRequested.emit();
  }

  removeSelectedFile(event?: MouseEvent): void {
    event?.stopPropagation();
    this.resetSelection();
  }

  get selectedFileSizeLabel(): string {
    if (!this.selectedFile) {
      return '';
    }

    const sizeInMb = this.selectedFile.size / (1024 * 1024);
    return `${sizeInMb.toFixed(2)} MB`;
  }

  get selectedFileExtensionLabel(): string {
    if (!this.selectedFile) {
      return '--';
    }

    return this.getExtension(this.selectedFile.name).replace('.', '').toUpperCase();
  }

  get selectedFileIconType(): 'pdf' | 'doc' | 'docx' | 'unknown' {
    if (!this.selectedFile) {
      return 'unknown';
    }

    const extension = this.getExtension(this.selectedFile.name);
    if (extension === '.pdf') {
      return 'pdf';
    }

    if (extension === '.doc') {
      return 'doc';
    }

    if (extension === '.docx') {
      return 'docx';
    }

    return 'unknown';
  }

  get selectedFileLastModifiedLabel(): string {
    if (!this.selectedFile) {
      return '--';
    }

    return new Intl.DateTimeFormat('vi-VN').format(new Date(this.selectedFile.lastModified));
  }

  private setSelectedFile(file: File | null): void {
    if (!file) {
      this.resetSelection();
      return;
    }

    const extension = this.getExtension(file.name);
    if (!this.acceptedExtensions.includes(extension)) {
      this.resetSelection();
      this.errorMessage = this.i18n.translate('cvUploadModal.unsupportedFormat');
      return;
    }

    if (file.size > this.maxFileSize) {
      this.resetSelection();
      this.errorMessage = this.i18n.translate('cvUploadModal.fileTooLarge');
      return;
    }

    if (this.selectedFile === file && (this.isUploadingToTemp || this.stagedUploadId !== null)) {
      return;
    }

    this.uploadSubscription?.unsubscribe();
    this.selectedFile = file;
    this.stagedUploadId = null;
    this.errorMessage = '';
    this.isUploadingToTemp = true;

    this.uploadSubscription = this.resumeService.stageCvToTemp(file).subscribe({
      next: staged => {
        if (this.selectedFile !== file) {
          return;
        }
        this.stagedUploadId = staged.uploadId;
        this.isUploadingToTemp = false;
      },
      error: err => {
        if (this.selectedFile !== file) {
          return;
        }
        this.selectedFile = null;
        this.stagedUploadId = null;
        this.isUploadingToTemp = false;
        this.errorMessage = err?.error?.message || this.i18n.translate('cvUploadModal.tempUploadFailed');
      }
    });
  }

  private getExtension(fileName: string): string {
    return `.${fileName.split('.').pop()?.toLowerCase() ?? ''}`;
  }

  private resetSelection(): void {
    this.uploadSubscription?.unsubscribe();
    this.selectedFile = null;
    this.stagedUploadId = null;
    this.isUploadingToTemp = false;
    this.errorMessage = '';
    this.enableAiAnalysis = false;
    this.isSubmitting = false;

    if (this.cvUploadInput) {
      this.cvUploadInput.nativeElement.value = '';
    }
  }
}
