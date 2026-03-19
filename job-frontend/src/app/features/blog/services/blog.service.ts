import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
    );
  }

  commentList(pageIndex: number, pageSize: number): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blogs/comments/${pageIndex}/${pageSize}`
    );
  }

  postBlog(blog: any) {
    return this.http.post<any>(
      `${this.url}/blogs`,
      blog,
      { withCredentials: true }
    );
  }
  blogDetail(blogId: bigint) {
    return this.http.get<any>(
      `${this.url}/blogs/${blogId}`
    );
  }
  like(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blogs/like`,
      blogId,
      { withCredentials: true }
    );
  }
  unlike(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blogs/unlike`,
      blogId,
      { withCredentials: true }
    );
  }
  comment(comment: any) {
    return this.http.post<any>(
      `${this.url}/blogs/comment`,
      comment,
      { withCredentials: true }
    );
  }
}
