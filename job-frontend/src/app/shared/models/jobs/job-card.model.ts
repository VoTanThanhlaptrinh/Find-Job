export interface JobCardModel {
  id: string | number;
  title: string;
  address: string;
  salary: number | string;
  time: string;
  companyName?: string;
  logoUrl?: string;
  skills?: string[];
  level?: string;
  experience?: string;
  postedAt?: string;
  deadline?: string;
  isSaved?: boolean;
}
