import { Component, effect, OnInit, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ResumeService, UploadingFileState } from '../../../../core/services/resume.service';
import { LoadingComponent } from '../../../../shared/components/loading/loading.component';
import { ResumeReviewComponent } from '../../../../shared/components/resume-review/resume-review.component';
import { SkeletonCvCardComponent } from '../../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeReviewInput } from '../../../../shared/models/jobs/resume-review-input.model';
import { CvUploadModalComponent } from '../cv-upload-modal/cv-upload-modal.component';
import { I18nService } from '../../../../core/i18n/i18n.service';

@Component({
  selector: 'app-cv-ui',
  standalone: true,
  imports: [CommonModule, ResumeReviewComponent, LoadingComponent, SkeletonCvCardComponent, CvUploadModalComponent],
  templateUrl: './cv-ui.component.html',
  styleUrl: './cv-ui.component.css'
})
export class CvUiComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);
  private readonly i18n = inject(I18nService);

  resumes: ResumeReviewInput[] = [];
  isLoading = false;
  isUploadModalOpen = false;
  uploadingFile: UploadingFileState | null = null;
  readonly skeleton = true;
  readonly skeletonRows = [1, 2, 3];

  readonly title = computed(() => this.i18n.translate('cvList.title'));
  readonly subtitle = computed(() => this.i18n.translate('cvList.subtitle'));
  readonly addNewLabel = computed(() => this.i18n.translate('cvList.addNew'));
  readonly totalCvLabel = computed(() => this.i18n.translate('cvList.totalCv'));
  readonly pdfCvLabel = computed(() => this.i18n.translate('cvList.pdfCv'));
  readonly docCvLabel = computed(() => this.i18n.translate('cvList.docCv'));
  readonly latestUpdateLabel = computed(() => this.i18n.translate('cvList.latestUpdate'));
  readonly analyzingTitle = computed(() => this.i18n.translate('cvList.analyzingTitle'));
  readonly analyzingDesc = computed(() => this.i18n.translate('cvList.analyzingDesc'));
  readonly uploadingLabel = computed(() => this.i18n.translate('cvList.uploading'));
  readonly uploadSuccessLabel = computed(() => this.i18n.translate('cvList.uploadSuccess'));
  readonly analyzedSuccessLabel = computed(() => this.i18n.translate('cvList.analyzedSuccess'));
  readonly uploadFailedLabel = computed(() => this.i18n.translate('cvList.uploadFailed'));
  readonly noCvLabel = computed(() => this.i18n.translate('cvList.noCv'));

  constructor() {
    effect(() => {
      this.resumes = this.resumeService.resumes$();
      this.isLoading = false;
    });
    effect(() => {
      this.uploadingFile = this.resumeService.uploadingFile$();
    });
  }

  ngOnInit(): void {
    this.loadResumes();
  }

  private loadResumes(): void {
    this.isLoading = true;
    this.resumeService.getUserResumes();
  }

  get totalResumes(): number {
    return this.resumes.length;
  }

  get pdfCount(): number {
    return this.resumes.filter(resume => resume.fileName.toLowerCase().endsWith('.pdf')).length;
  }

  get docCount(): number {
    return this.resumes.filter(resume => {
      const lowerFileName = resume.fileName.toLowerCase();
      return lowerFileName.endsWith('.doc') || lowerFileName.endsWith('.docx');
    }).length;
  }

  get latestCvDateLabel(): string {
    if (this.resumes.length === 0) {
      return '--';
    }

    const latestResume = [...this.resumes].sort((a, b) => {
      return new Date(b.createDate).getTime() - new Date(a.createDate).getTime();
    })[0];

    const latestDate = new Date(latestResume.createDate);
    if (Number.isNaN(latestDate.getTime())) {
      return latestResume.createDate;
    }

    return new Intl.DateTimeFormat('vi-VN').format(latestDate);
  }

  openUploadModal(): void {
    this.isUploadModalOpen = true;
  }

  closeUploadModal(): void {
    this.isUploadModalOpen = false;
  }

  submitUploadCv(file: File): void {
    this.resumeService.postResume(file);
    this.closeUploadModal();
  }
}
