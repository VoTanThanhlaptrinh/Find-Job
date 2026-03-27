import { ApiResponse } from '../api-response.model';
import { JobCardModel } from './job-card.model';

export interface PagedPayload<T> {
  content: T[];
  pageable?: {
    sort?: {
      empty?: boolean;
      unsorted?: boolean;
      sorted?: boolean;
    };
    offset?: number;
    pageNumber?: number;
    pageSize?: number;
    paged?: boolean;
    unpaged?: boolean;
  };
  totalPages?: number;
  totalElements?: number;
  last?: boolean;
  size?: number;
  number?: number;
  sort?: {
    empty?: boolean;
    unsorted?: boolean;
    sorted?: boolean;
  };
  numberOfElements?: number;
  first?: boolean;
  empty?: boolean;
}

export interface HomeInitViewModel {
  jobSoon: PagedPayload<JobCardModel>;
}

export interface AddressCountViewModel {
  city: string;
  count: number;
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
  address: string[];
  times: string[];
  title: string;
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
