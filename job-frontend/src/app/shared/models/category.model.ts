export interface Category {
  id: number;
  name: string;
  parentId?: number;
  createdAt?: string;
  updatedAt?: string;
  status?: string;
}

export interface CategoryRequest {
  name: string;
  parentId?: number;
}
