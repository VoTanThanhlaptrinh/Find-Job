export interface ResumeUploadInitiateRequest {
  fileName: string;
  contentType: string;
  size: number;
}

export interface ResumeUploadInitiateResponse {
  uploadId: string;
  uploadUrl: string;
  httpMethod: string;
  requiredHeaders: Record<string, string>;
  expiresAt: string;
}
