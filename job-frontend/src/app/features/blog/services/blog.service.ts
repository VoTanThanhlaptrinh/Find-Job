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
  blogList(pageIndex: number, pageSize: number): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blogs/page/${pageIndex}/${pageSize}`
    ).pipe(take(1));
  }

  commentList(pageIndex: number, pageSize: number): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blogs/comments/${pageIndex}/${pageSize}`
    ).pipe(take(1));
  }

  postBlog(blog: any) {
    return this.http.post<any>(
      `${this.url}/blogs`,
      blog,
      { withCredentials: true }
    ).pipe(take(1));
  }
  blogDetail(blogId: bigint) {
    return this.http.get<any>(
      `${this.url}/blogs/${blogId}`
    ).pipe(take(1));
  }
  like(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blogs/like`,
      blogId,
      { withCredentials: true }
    ).pipe(take(1));
  }
  unlike(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blogs/unlike`,
      blogId,
      { withCredentials: true }
    ).pipe(take(1));
  }
  comment(comment: any) {
    return this.http.post<any>(
      `${this.url}/blogs/comment`,
      comment,
      { withCredentials: true }
    ).pipe(take(1));
  }
}
