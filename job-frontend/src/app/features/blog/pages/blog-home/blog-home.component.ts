import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { BlogService } from '../../services/blog.service';
import { Router, RouterLink } from '@angular/router';
import { SkeletonBlogCardComponent } from '../../../../shared/components/skeleton-blog-card/skeleton-blog-card.component';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';

@Component({
  selector: 'app-blog-home',
  imports: [RouterLink, SkeletonBlogCardComponent, FormsModule],
  standalone: true,
  templateUrl: './blog-home.component.html',
  styleUrl: './blog-home.component.css'
})
export class BlogHomeComponent implements OnInit {
  @ViewChild('searchInput') searchInputElement?: ElementRef<HTMLInputElement>;

  pageIndex = 0;
  pageSize = 6;
  totalElements = 0;
  searchTerm = '';
  appliedSearchTerm = '';
  dateOrder = 'desc';
  blogList: any[] = [];
  isLoading = false;
  isLoadMore = false;

  constructor(
    private blogService: BlogService,
    private authService: AuthService,
    private tokenService: TokenService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.getBlogList(this.pageIndex, this.pageSize, this.searchTerm);
  }

  getBlogList(pageIndex: number, pageSize: number, title: string = '', loadMore: boolean = false): void {
    if (loadMore) {
      this.isLoadMore = true;
    } else {
      this.isLoading = true;
    }

    this.blogService.blogList(pageIndex, pageSize, title, this.dateOrder).subscribe({
      next: (res) => {
        let newItems: any[] = [];
        if (res?.data?.content) {
          this.totalElements = res.data.page.totalElements || 0;
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
          this.blogList = [...this.blogList, ...newItems];
        } else {
          this.blogList = newItems;
          if (!res?.data?.content) {
            this.totalElements = 0;
          }
        }

        this.isLoading = false;
        this.isLoadMore = false;
      },
      error: (error) => {
        console.error('Error fetching blogs:', error);
        if (!loadMore) {
          this.blogList = [];
        }
        this.isLoading = false;
        this.isLoadMore = false;
      }
    });
  }

  onSearch(keyword: string): void {
    this.searchTerm = keyword.trim();
    this.appliedSearchTerm = this.searchTerm;
    if (this.searchInputElement) {
      this.searchInputElement.nativeElement.value = this.searchTerm;
    }
    this.pageIndex = 0;
    this.getBlogList(this.pageIndex, this.pageSize, this.appliedSearchTerm);
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.appliedSearchTerm = '';
    if (this.searchInputElement) {
      this.searchInputElement.nativeElement.value = '';
    }
    this.pageIndex = 0;
    this.getBlogList(this.pageIndex, this.pageSize, '');
  }

  clearInput(): void {
    this.searchTerm = '';
    if (this.searchInputElement) {
      this.searchInputElement.nativeElement.value = '';
      this.searchInputElement.nativeElement.focus();
    }
  }

  onDateOrderChange(order: string): void {
    if (this.dateOrder === order) return;
    this.dateOrder = order;
    this.pageIndex = 0;
    this.getBlogList(this.pageIndex, this.pageSize, this.appliedSearchTerm);
  }

  onCreateBlog(): void {
    if (this.tokenService.getToken() && this.authService.isLoggedIn()) {
      this.router.navigate(['/blog-creation']);
    } else {
      this.router.navigate(['/'], { queryParams: { auth: 'login' } });
    }
  }

  loadMore(): void {
    this.pageIndex++;
    this.getBlogList(this.pageIndex, this.pageSize, this.appliedSearchTerm, true);
  }
}

