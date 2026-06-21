import {Component, OnInit} from '@angular/core';
import {BlogService} from '../../services/blog.service';
import {MatPaginatorModule} from '@angular/material/paginator';
import {RouterLink} from '@angular/router';
import {SkeletonBlogCardComponent} from '../../../../shared/components/skeleton-blog-card/skeleton-blog-card.component';

@Component({
  selector: 'app-blog-home',
  imports: [MatPaginatorModule, RouterLink, SkeletonBlogCardComponent],
  standalone: true,
  templateUrl: './blog-home.component.html',
  styleUrl: './blog-home.component.css'
})
export class BlogHomeComponent implements OnInit{
  pageIndex = 0;
  pageSize = 10;
  blogList:any;
  isLoading = false;
  constructor(private blogService:BlogService) {
  }
    ngOnInit(): void {
      this.getBlogList(this.pageIndex,this.pageSize);
    }
  getBlogList(pageIndex:number, pageSize:number){
     this.isLoading = true;
     this.blogService.blogList(pageIndex,pageSize).subscribe({
       next: (res) => {
         this.blogList = res.data.content.map((item: any) => ({
           id: item.id,
           amount: item.amountLike,
           description: item.description,
           image: "assets/web_css/img/post.png",
           title: item.title,
           createDate: item.create_date,
         }));
         this.isLoading = false;
       },
       error: (error) => {
         console.error('Error fetching jobs:', error);
         this.isLoading = false;
       }
     });
  }
}
