import { Component, OnInit } from '@angular/core';
import { BlogService } from '../../services/blog.service';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';
import { SkeletonBlogCardComponent } from '../../../../shared/components/skeleton-blog-card/skeleton-blog-card.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-blog-home',
  imports: [MatPaginatorModule, RouterLink, SkeletonBlogCardComponent, FormsModule],
  standalone: true,
  templateUrl: './blog-home.component.html',
  styleUrl: './blog-home.component.css'
})
export class BlogHomeComponent implements OnInit {
  pageIndex = 0;
  pageSize = 6;
  totalElements = 0;
  searchTerm = '';
  blogList: any[] = [];
  isLoading = false;

  constructor(private blogService: BlogService) {}

  ngOnInit(): void {
    this.getBlogList(this.pageIndex, this.pageSize, this.searchTerm);
  }

  getBlogList(pageIndex: number, pageSize: number, title: string = ''): void {
    this.isLoading = true;
    this.blogService.blogList(pageIndex, pageSize, title).subscribe({
      next: (res) => {
        if (res?.data?.content) {
          this.totalElements = res.data.totalElements || 0;
          this.blogList = res.data.content.map((item: any) => ({
            id: item.id,
            amount: item.amountLike,
            description: item.description,
            image: item.image || 'assets/web_css/img/blog/p1.jpg',
            title: item.title,
            authorName: item.authorName,
            createDate: item.createDate || item.create_date,
          }));
        } else {
          this.blogList = [];
          this.totalElements = 0;
        }
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error fetching blogs:', error);
        this.blogList = [];
        this.isLoading = false;
      }
    });
  }

  onSearch(keyword: string): void {
    this.searchTerm = keyword.trim();
    this.pageIndex = 0;
    this.getBlogList(this.pageIndex, this.pageSize, this.searchTerm);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.getBlogList(this.pageIndex, this.pageSize, this.searchTerm);
  }
}

