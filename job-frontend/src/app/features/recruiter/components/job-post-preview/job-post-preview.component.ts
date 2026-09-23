import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { MarkdownViewerComponent } from '../../../../shared/components/markdown-viewer/markdown-viewer.component';

export interface JobPreviewData {
  jobName: string;
  categoryName: string;
  jobType: string;
  jobTypeLabel: string;
  salary: string;
  address: string;
  headcount: number | null;
  deadlineCV: string;
  enableAiAnalysis: boolean;
  jobDescription: string;
  jobRequirement: string;
  jobSkill: string;
  moreDetail: string;
}

@Component({
  selector: 'app-job-post-preview',
  standalone: true,
  imports: [
    CommonModule,
    TranslatePipe,
    MarkdownViewerComponent
  ],
  templateUrl: './job-post-preview.component.html',
  styleUrl: './job-post-preview.component.css'
})
export class JobPostPreviewComponent {
  @Input() isOpen = false;
  @Input() isSubmitting = false;
  @Input() data: JobPreviewData | null = null;

  @Output() backToEdit = new EventEmitter<void>();
  @Output() saveJob = new EventEmitter<void>();

  onBack(): void {
    this.backToEdit.emit();
  }

  onSave(): void {
    this.saveJob.emit();
  }
}
