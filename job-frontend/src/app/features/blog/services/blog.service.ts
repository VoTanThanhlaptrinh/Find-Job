import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, take } from 'rxjs';
import { UtilitiesService } from '../../../core/services/utilities.service';

@Injectable({
  providedIn: 'root',
})
export class BlogService {
  private url: string;

  constructor(private http: HttpClient, private utilities: UtilitiesService) {
    this.url = utilities.getURLDev();
  }

  blogList(pageIndex: number = 0, pageSize: number = 10, title: string = '', dateOrder: string = 'desc'): Observable<any> {
    const query = new URLSearchParams({
      pageIndex: pageIndex.toString(),
      pageSize: pageSize.toString(),
      title: title || '',
      dateOrder: dateOrder || 'desc',
    }).toString();
    return this.http.get<any>(`${this.url}/blogs?${query}`).pipe(take(1));
  }

  myBlogList(pageIndex: number = 0, pageSize: number = 10, title: string = '', dateOrder: string = 'desc'): Observable<any> {
    const query = new URLSearchParams({
      pageIndex: pageIndex.toString(),
      pageSize: pageSize.toString(),
      title: title || '',
      dateOrder: dateOrder || 'desc',
    }).toString();
    return this.http.get<any>(`${this.url}/blogs/me?${query}`, { withCredentials: true }).pipe(take(1));
  }

  commentList(pageIndex: number = 0, pageSize: number = 10): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blogs/comments/${pageIndex}/${pageSize}`
    ).pipe(take(1));
  }

  postBlog(blog: any): Observable<any> {
    return this.http.post<any>(
      `${this.url}/blogs`,
      blog,
      { withCredentials: true }
    ).pipe(take(1));
  }

  updateBlog(blogId: number | bigint, blog: any): Observable<any> {
    return this.http.put<any>(
      `${this.url}/blogs/${blogId}`,
      blog,
      { withCredentials: true }
    ).pipe(take(1));
  }

  deleteBlog(blogId: number | bigint): Observable<any> {
    return this.http.delete<any>(
      `${this.url}/blogs/${blogId}`,
      { withCredentials: true }
    ).pipe(take(1));
  }

  blogDetail(blogId: number | bigint): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blogs/${blogId}`
    ).pipe(take(1));
  }

  like(blogId: number | bigint): Observable<any> {
    return this.http.post<any>(
      `${this.url}/blogs/likes`,
      { id: Number(blogId) },
      { withCredentials: true }
    ).pipe(take(1));
  }

  unlike(blogId: number | bigint): Observable<any> {
    return this.http.post<any>(
      `${this.url}/blogs/likes/remove`,
      { id: Number(blogId) },
      { withCredentials: true }
    ).pipe(take(1));
  }

  comment(comment: any): Observable<any> {
    return this.http.post<any>(
      `${this.url}/blogs/comments`,
      comment,
      { withCredentials: true }
    ).pipe(take(1));
  }
}

