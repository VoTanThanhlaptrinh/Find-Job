import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, Injectable, signal } from '@angular/core';
import { finalize, take, tap } from 'rxjs';
import { UtilitiesService } from './utilities.service';
import { ApiResponse } from '../../shared/models/api-response.model';
import { Category, CategoryRequest } from '../../shared/models/category.model';
import { NotifyMessageService } from './notify-message.service';

@Injectable({
  providedIn: 'root',
})
export class CategoryService {
  private url: string;

  private categoriesData = signal<Category[]>([]);
  private loadingCategories = signal<boolean>(false);
  categories = computed(() => this.categoriesData());
  isLoadingCategories = computed(() => this.loadingCategories());

  constructor(
    private http: HttpClient,
    private utilities: UtilitiesService,
    private notify: NotifyMessageService
  ) {
    this.url = utilities.getURLDev();
  }

  loadCategories(): void {
    this.loadingCategories.set(true);
    this.http
      .get<ApiResponse<Category[]>>(`${this.url}/v1/categories`)
      .pipe(
        take(1),
        finalize(() => this.loadingCategories.set(false))
      )
      .subscribe({
        next: (response) => {
          this.categoriesData.set(response.data);
        },
        error: (error) => {
          console.error('Error fetching categories:', error);
          this.notify.error('Lỗi khi tải danh sách danh mục');
        },
      });
  }

  createCategory(request: CategoryRequest): void {
    this.loadingCategories.set(true);
    this.http
      .post<ApiResponse<Category>>(`${this.url}/admin/categories`, request)
      .pipe(
        take(1),
        finalize(() => this.loadingCategories.set(false))
      )
      .subscribe({
        next: (response) => {
          this.categoriesData.update((cats) => [...cats, response.data]);
          this.notify.success('Tạo danh mục thành công');
        },
        error: (error) => {
          console.error('Error creating category:', error);
          this.notify.error('Lỗi khi tạo danh mục');
        },
      });
  }

  updateCategory(id: number, request: CategoryRequest): void {
    this.loadingCategories.set(true);
    this.http
      .put<ApiResponse<Category>>(`${this.url}/admin/categories/${id}`, request)
      .pipe(
        take(1),
        finalize(() => this.loadingCategories.set(false))
      )
      .subscribe({
        next: (response) => {
          this.categoriesData.update((cats) =>
            cats.map((c) => (c.id === id ? response.data : c))
          );
          this.notify.success('Cập nhật danh mục thành công');
        },
        error: (error) => {
          console.error('Error updating category:', error);
          this.notify.error('Lỗi khi cập nhật danh mục');
        },
      });
  }

  deleteCategory(id: number): void {
    this.loadingCategories.set(true);
    this.http
      .delete<ApiResponse<any>>(`${this.url}/admin/categories/${id}`)
      .pipe(
        take(1),
        finalize(() => this.loadingCategories.set(false))
      )
      .subscribe({
        next: () => {
          this.categoriesData.update((cats) => cats.filter((c) => c.id !== id));
          this.notify.success('Xóa danh mục thành công');
        },
        error: (error) => {
          console.error('Error deleting category:', error);
          this.notify.error(
            error?.error?.message || 'Lỗi khi xóa danh mục'
          );
        },
      });
  }
}
