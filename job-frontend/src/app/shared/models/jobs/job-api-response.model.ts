import { ApiResponse } from '../api-response.model';
import { JobCardModel } from './job-card.model';

export interface PagedPayload<T> {
  content: T[];
}

export interface HomeInitViewModel {
  jobSalary: PagedPayload<JobCardModel>;
  jobSoon: PagedPayload<JobCardModel>;
}

export interface AddressCountViewModel {
  address: string;
  amount: number;
}

export interface JobDetailViewModel {
  id: string | number;
  title: string;
  address: string;
  description: string;
  salary: number;
  time: string;
  requireDetails: string;
  skill: string;
  expiredDate: string;
}

export interface HirerJobViewModel {
  id: string | number;
  title: string;
  description: string;
  address: string;
  salary: number;
  time: string;
  applies: number;
}

export interface JobFilterPayload {
  pageIndex: number;
  pageSize: number;
  min: number;
  max: number;
  address: string[];
  times: string[];
}

export type HomeInitApiResponse = ApiResponse<HomeInitViewModel>;
export type JobListApiResponse = ApiResponse<PagedPayload<JobCardModel>>;
export type JobCountApiResponse = ApiResponse<number>;
export type JobAddressCountApiResponse = ApiResponse<AddressCountViewModel[]>;
export type JobDetailApiResponse = ApiResponse<JobDetailViewModel>;
export type JobExistsApiResponse = ApiResponse<boolean>;
export type JobSubmitApiResponse = ApiResponse<string | null>;
export type HirerJobListApiResponse = ApiResponse<PagedPayload<HirerJobViewModel>>;
export type HirerJobCountApiResponse = ApiResponse<number>;
