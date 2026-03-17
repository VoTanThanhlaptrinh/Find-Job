export interface ApplyCvWithExistingRequest {
  jobId: number;
  existingCvId: number;
  email: string;
  coverLetter: string;
}

export interface ApplyCvWithUploadRequest {
  jobId: number;
  cvFile: File;
  email: string;
  coverLetter: string;
}

export type ApplyCvRequest =
  | ApplyCvWithExistingRequest
  | ApplyCvWithUploadRequest;

export interface ApplyCvWithExistingResponse {
  applyType: 'existing';
  applicationId: string;
  jobId: string;
  existingCvId: number;
  status: string;
  submittedAt: string;
}

export interface ApplyCvWithUploadResponse {
  applyType: 'upload';
  applicationId: string;
  jobId: string;
  uploadedCvUrl: string;
  status: string;
  submittedAt: string;
}

export type ApplyCvResponse =
  | ApplyCvWithExistingResponse
  | ApplyCvWithUploadResponse;
