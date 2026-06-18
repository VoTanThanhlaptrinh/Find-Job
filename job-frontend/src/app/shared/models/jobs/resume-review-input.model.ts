export interface ResumeReviewInput {
  id: number;
  fileName: string;
  createDate: string;
  isAnalyzed: boolean;
  isNewlyUploaded?: boolean;
}
