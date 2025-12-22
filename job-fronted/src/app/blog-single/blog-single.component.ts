import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {BlogService} from '../services/blog.service';
import {take} from 'rxjs';

@Component({
  selector: 'app-blog-single',
  imports: [
    RouterLink
  ],
  standalone: true,
  templateUrl: './blog-single.component.html',
  styleUrl: './blog-single.component.css'
})
export class BlogSingleComponent implements OnInit{
  blog:any;
  id: any;
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
      this.blogService.blogDetail(id).pipe(take(1)).subscribe({
        next: res =>{
          this.blog = res.data;
        },
        error: err => {
          console.error(err);
        }
        }
      );
    }
}
