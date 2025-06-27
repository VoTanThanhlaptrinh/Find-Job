import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BlogService {

  constructor(private http: HttpClient) {}
  blogList(pageIndex:number,pageSize:number): Observable<any>{
    return this.http.get<any>(`http://localhost:8080/api/blog/pub/blogList/${pageIndex}/${pageSize}`,);
  }

  commentList(pageIndex:number,pageSize:number):Observable<any> {
    return this.http.get<any>(`http://localhost:8080/api/blog/pub/commentList/${pageIndex}/${pageSize}`);
  }

  postBlog(blog:any) {
    return this.http.post<any>(`http://localhost:8080/api/blog/pri/postBlog`,blog);
  }
  blogDetail(blogId: bigint) {
    return this.http.get<any>(`http://localhost:8080/api/blog/pub/blogDetail/${blogId}`);
  }
  like(blogId: bigint) {
    return this.http.post<any>(`http://localhost:8080/api/blog/pri/like`,blogId);
  }
  unlike(blogId: bigint) {
    return this.http.post<any>(`http://localhost:8080/api/blog/pri/unlike`,blogId);
  }
  comment(comment:any) {
    return this.http.post<any>(`http://localhost:8080/api/blog/pri//pri/comment`,comment);
  }
  filterWithAddressTimeSalary(filter:any){
    return this.http.post<any>(`http://localhost:8080/api/job/pub/filterWithAddressTimeSalary`,filter);
  }
}
