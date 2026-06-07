import { Component, effect, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SkeletonCvCardComponent } from '../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeService } from '../../../core/services/resume.service';
import { ResumeReviewComponent } from '../../../shared/components/resume-review/resume-review.component';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { CvUploadModalComponent } from '../cv-upload-modal/cv-upload-modal.component';
import { ResumeReviewInput } from '../../../shared/models/jobs/resume-review-input.model';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { FileMessage } from '../../../shared/models/sse/sse.model';

@Component({
  selector: 'app-cv-ui',
  standalone: true,
  imports: [CommonModule, ResumeReviewComponent, LoadingComponent, SkeletonCvCardComponent, CvUploadModalComponent, TranslatePipe],
  templateUrl: './cv-ui.component.html',
  styleUrl: './cv-ui.component.css'
})
export class CvUiComponent implements OnInit {
  private readonly resumeService = inject(ResumeService);

  resumes: ResumeReviewInput[] = [];
  isLoading = false;
  isUploadModalOpen = false;
  uploadingFile: FileMessage | null = null;
  readonly skeleton = true;
  readonly skeletonRows = [1, 2, 3];

  constructor() {
    effect(() => {
      this.resumes = this.resumeService.resumes$();
      this.isLoading = false;
      this.uploadingFile = this.resumeService.uploadingFile$();
      console.log(this.resumeService.uploadingFile$());
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
