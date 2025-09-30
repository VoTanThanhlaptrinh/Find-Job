import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UtilitiesService } from './utilities.service';

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
      `${this.url}/blog/pub/blogList/${pageIndex}/${pageSize}`
    );
  }

  commentList(pageIndex: number, pageSize: number): Observable<any> {
    return this.http.get<any>(
      `${this.url}/blog/pub/commentList/${pageIndex}/${pageSize}`
    );
  }

  postBlog(blog: any) {
    return this.http.post<any>(
      `${this.url}/blog/pri/postBlog`,
      blog,
      { withCredentials: true }
    );
  }
  blogDetail(blogId: bigint) {
    return this.http.get<any>(
      `${this.url}/blog/pub/blogDetail/${blogId}`
    );
  }
  like(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blog/pri/like`,
      blogId,
      { withCredentials: true }
    );
  }
  unlike(blogId: bigint) {
    return this.http.post<any>(
      `${this.url}/blog/pri/unlike`,
      blogId,
      { withCredentials: true }
    );
  }
  comment(comment: any) {
    return this.http.post<any>(
      `${this.url}/blog/pri/comment`,
      comment,
      { withCredentials: true }
    );
  }
}
