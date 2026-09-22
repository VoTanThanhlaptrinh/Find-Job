import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BlogService } from '../../services/blog.service';
import { take } from 'rxjs';
import { SkeletonBlogSingleComponent } from '../../../../shared/components/skeleton-blog-single/skeleton-blog-single.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-blog-single',
  imports: [
    RouterLink,
    SkeletonBlogSingleComponent,
    CommonModule
  ],
  standalone: true,
  templateUrl: './blog-single.component.html',
  styleUrl: './blog-single.component.css'
})
export class BlogSingleComponent implements OnInit {
  blog: any;
  id: number = 0;
  isLoading = false;
  isLiked = false;
  likeLoading = false;

  constructor(
    private route: ActivatedRoute,
    private blogService: BlogService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe((params) => {
      this.id = Number(params['id']);
      if (this.id) {
        this.getBlogDetail(this.id);
      }
    });
  }

  getBlogDetail(id: number): void {
    this.isLoading = true;
    this.blogService.blogDetail(id).pipe(take(1)).subscribe({
      next: (res) => {
        this.blog = res.data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error fetching blog detail:', err);
        this.isLoading = false;
      }
    });
  }

  toggleLike(): void {
    if (!this.blog || this.likeLoading) return;
    this.likeLoading = true;
    if (!this.isLiked) {
      this.blogService.like(this.id).subscribe({
        next: () => {
          this.isLiked = true;
          this.blog.amountLike = (this.blog.amountLike || 0) + 1;
          this.likeLoading = false;
        },
        error: (err) => {
          console.error('Error liking blog:', err);
          this.likeLoading = false;
        }
      });
    } else {
      this.blogService.unlike(this.id).subscribe({
        next: () => {
          this.isLiked = false;
          this.blog.amountLike = Math.max(0, (this.blog.amountLike || 0) - 1);
          this.likeLoading = false;
        },
        error: (err) => {
          console.error('Error unliking blog:', err);
          this.likeLoading = false;
        }
      });
    }
  }
}

