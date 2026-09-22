import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { BlogService } from '../../../../blog/services/blog.service';

@Component({
  selector: 'app-profile-my-blog',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './profile-my-blog.component.html',
  styleUrl: './profile-my-blog.component.css'
})
export class ProfileMyBlogComponent implements OnInit {
  private readonly blogService = inject(BlogService);
  private readonly router = inject(Router);

  @Output() totalBlogsChange = new EventEmitter<number>();

  myBlogList: any[] = [];
  isLoadingMyBlogs = false;
  isLoadMoreMyBlogs = false;
  blogPageIndex = 0;
  blogPageSize = 6;
  blogTotalElements = 0;
  blogSearchTerm = '';
  blogAppliedSearchTerm = '';
  blogDateOrder = 'desc';

  ngOnInit(): void {
    this.loadMyBlogs(0, this.blogPageSize, this.blogSearchTerm, this.blogDateOrder);
  }

  loadMyBlogs(pageIndex: number, pageSize: number, title: string = '', dateOrder: string = 'desc', loadMore: boolean = false): void {
    if (loadMore) {
      this.isLoadMoreMyBlogs = true;
    } else {
      this.isLoadingMyBlogs = true;
    }

    this.blogService.myBlogList(pageIndex, pageSize, title, dateOrder).subscribe({
      next: (res) => {
        let newItems: any[] = [];
        if (res?.data?.content) {
          this.blogTotalElements = res.data.page?.totalElements ?? res.data.content.length ?? 0;
          this.totalBlogsChange.emit(this.blogTotalElements);
          newItems = res.data.content.map((item: any) => ({
            id: item.id,
            amount: item.amountLike,
            description: item.description,
            image: item.image || 'assets/web_css/img/blog/p1.jpg',
            title: item.title,
            authorName: item.authorName,
            createDate: item.createDate || item.create_date,
          }));
        }

        if (loadMore) {
          this.myBlogList = [...this.myBlogList, ...newItems];
        } else {
          this.myBlogList = newItems;
          if (!res?.data?.content) {
            this.blogTotalElements = 0;
            this.totalBlogsChange.emit(0);
          }
        }

        this.isLoadingMyBlogs = false;
        this.isLoadMoreMyBlogs = false;
      },
      error: (err) => {
        console.error('Error fetching my blogs:', err);
        if (!loadMore) {
          this.myBlogList = [];
          this.blogTotalElements = 0;
          this.totalBlogsChange.emit(0);
        }
        this.isLoadingMyBlogs = false;
        this.isLoadMoreMyBlogs = false;
      }
    });
  }

  onSearchBlog(keyword: string): void {
    this.blogSearchTerm = keyword.trim();
    this.blogAppliedSearchTerm = this.blogSearchTerm;
    this.blogPageIndex = 0;
    this.loadMyBlogs(this.blogPageIndex, this.blogPageSize, this.blogAppliedSearchTerm, this.blogDateOrder);
  }

  onClearSearchBlog(input?: HTMLInputElement): void {
    this.blogSearchTerm = '';
    this.blogAppliedSearchTerm = '';
    if (input) {
      input.value = '';
    }
    this.blogPageIndex = 0;
    this.loadMyBlogs(this.blogPageIndex, this.blogPageSize, '', this.blogDateOrder);
  }

  onBlogDateOrderChange(order: string): void {
    if (this.blogDateOrder === order) return;
    this.blogDateOrder = order;
    this.blogPageIndex = 0;
    this.loadMyBlogs(this.blogPageIndex, this.blogPageSize, this.blogAppliedSearchTerm, this.blogDateOrder);
  }

  loadMoreMyBlogs(): void {
    this.blogPageIndex++;
    this.loadMyBlogs(this.blogPageIndex, this.blogPageSize, this.blogAppliedSearchTerm, this.blogDateOrder, true);
  }

  navigateToCreateBlog(): void {
    this.router.navigate(['/blog-creation']);
  }
}
