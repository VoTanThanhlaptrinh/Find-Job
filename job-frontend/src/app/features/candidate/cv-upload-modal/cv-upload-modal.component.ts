import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-cv-upload-modal',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './cv-upload-modal.component.html',
  styleUrl: './cv-upload-modal.component.css'
})
export class CvUploadModalComponent implements OnChanges {
  private readonly i18n = inject(I18nService);
  
  @ViewChild('cvUploadInput') cvUploadInput?: ElementRef<HTMLInputElement>;
  @Input() isOpen = false;
  @Output() closeRequested = new EventEmitter<void>();
  @Output() submitRequested = new EventEmitter<File>();

  selectedFile: File | null = null;
  errorMessage = '';
  readonly maxFileSize = 5 * 1024 * 1024;
  readonly acceptedExtensions = '.pdf,.doc,.docx';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && !this.isOpen) {
      this.resetSelection();
    }
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
    this.setSelectedFile(file);
  }

  onSubmit(): void {
    if (!this.selectedFile) {
      this.errorMessage = this.i18n.translate('cvUploadModal.selectFileHint');
      return;
    }

    this.submitRequested.emit(this.selectedFile);
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
      this.selectedFile = null;
      this.errorMessage = '';
      return;
    }

    const extension = this.getExtension(file.name);
    const isAllowedExtension = this.acceptedExtensions.includes(extension);

    if (!isAllowedExtension) {
      this.selectedFile = null;
      this.errorMessage = this.i18n.translate('cvUploadModal.unsupportedFormat');
      return;
    }

    if (file.size > this.maxFileSize) {
      this.selectedFile = null;
      this.errorMessage = this.i18n.translate('cvUploadModal.fileTooLarge');
      return;
    }

    this.selectedFile = file;
    this.errorMessage = '';
  }

  private getExtension(fileName: string): string {
    return `.${fileName.split('.').pop()?.toLowerCase() ?? ''}`;
  }

  private resetSelection(): void {
    this.selectedFile = null;
    this.errorMessage = '';

    if (this.cvUploadInput) {
      this.cvUploadInput.nativeElement.value = '';
    }
  }
}
