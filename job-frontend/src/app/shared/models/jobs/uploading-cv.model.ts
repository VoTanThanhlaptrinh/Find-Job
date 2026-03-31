export interface UploadingCv {
  tempId: string;
  fileName: string;
  uploadProgress: number;
  uploadStatus: 'uploading' | 'uploaded';
  analyzeStatus: 'idle' | 'analyzing' | 'done';
}
