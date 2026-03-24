import { Component, OnInit } from '@angular/core';
import { take } from 'rxjs';
import { ResumeService } from '../../../../core/services/resume.service';
import { LoadingComponent } from '../../../../shared/components/loading/loading.component';
import { ResumeReviewComponent } from '../../../../shared/components/resume-review/resume-review.component';
import { SkeletonCvCardComponent } from '../../../../shared/components/skeleton-cv-card/skeleton-cv-card.component';
import { ResumeReviewInput } from '../../../../shared/models/jobs/resume-review-input.model';
import { CvUploadModalComponent } from '../cv-upload-modal/cv-upload-modal.component';

@Component({
  selector: 'app-cv-ui',
  standalone: true,
  imports: [ResumeReviewComponent, LoadingComponent, SkeletonCvCardComponent, CvUploadModalComponent],
  templateUrl: './cv-ui.component.html',
  styleUrl: './cv-ui.component.css'
})
export class CvUiComponent implements OnInit {
  private readonly mockResumes: ResumeReviewInput[] = [
    { id: 1001, fileName: 'Nguyen_Frontend_2026.pdf', createDate: '2026-03-10T09:20:00' },
    { id: 1002, fileName: 'Tran_Backend_Profile.docx', createDate: '2026-03-06T14:05:00' },
    { id: 1003, fileName: 'Le_Fullstack_CV.pdf', createDate: '2026-02-28T19:40:00' }
  ];

  resumes: ResumeReviewInput[] = [...this.mockResumes];
  isLoading = false;
  isUploadModalOpen = false;
  readonly skeleton = true;
  readonly skeletonRows = [1, 2, 3];

  constructor(private readonly resumeService: ResumeService) {}

  ngOnInit(): void {
    this.loadResumes();
  }

  private loadResumes(): void {
    this.isLoading = true;
    this.resumeService.getUserResumes().pipe(take(1)).subscribe({
      next: (response) => {
        const apiResumes = response.data ?? [];
        this.resumes = apiResumes.length > 0 ? apiResumes : [...this.mockResumes];
        this.isLoading = false;
      },
      error: () => {
        this.resumes = [...this.mockResumes];
        this.isLoading = false;
      }
    });
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
    const maxId = this.resumes.reduce((currentMax, resume) => {
      return Math.max(currentMax, resume.id);
    }, 0);

    const newResume: ResumeReviewInput = {
      id: maxId + 1,
      fileName: file.name,
      createDate: new Date().toISOString()
    };

    this.resumes = [newResume, ...this.resumes];
    this.closeUploadModal();
  }

}
