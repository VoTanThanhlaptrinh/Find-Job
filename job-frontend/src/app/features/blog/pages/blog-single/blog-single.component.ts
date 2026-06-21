import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {BlogService} from '../../services/blog.service';
import {take} from 'rxjs';
import {SkeletonBlogSingleComponent} from '../../../../shared/components/skeleton-blog-single/skeleton-blog-single.component';

@Component({
  selector: 'app-blog-single',
  imports: [
    RouterLink,
    SkeletonBlogSingleComponent
  ],
  standalone: true,
  templateUrl: './blog-single.component.html',
  styleUrl: './blog-single.component.css'
})
export class BlogSingleComponent implements OnInit{
  blog:any;
  id: any;
  isLoading = false;
  constructor(private route: ActivatedRoute
              ,private blogService: BlogService) {
  }
    ngOnInit(): void {
        this.route.params.pipe(take(1)).subscribe(
          params =>{
             this.id = BigInt(params["id"]);
            this.getBlogDetail(this.id);
          }
        )
    }
    getBlogDetail(id: bigint){
      this.isLoading = true;
      this.blogService.blogDetail(id).pipe(take(1)).subscribe({
        next: res =>{
          this.blog = res.data;
          this.isLoading = false;
        },
        error: err => {
          console.error(err);
          this.isLoading = false;
        }
        }
      );
    }
}
