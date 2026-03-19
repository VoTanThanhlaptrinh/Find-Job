import { ApiResponse } from '../api-response.model';

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

export type ApplyCvWithExistingResponse = ApiResponse<string | null>;
export type ApplyCvWithUploadResponse = ApiResponse<string | null>;
export type ApplyCvResponse = ApiResponse<string | null>;
