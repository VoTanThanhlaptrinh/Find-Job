import {CommonModule, isPlatformBrowser} from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { CarouselModule } from 'ngx-owl-carousel-o';
import { HomeService } from '../services/home.service';
import { Router, RouterModule } from '@angular/router';
import { NotifyMessageService } from '../services/notify-message.service';
import { ToastrService } from 'ngx-toastr';
import {take} from 'rxjs';

@Component({
  selector: 'app-home',
  imports: [CommonModule, CarouselModule,RouterModule],
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
  relatedJobs: any;
  constructor(private homeService: HomeService,
    private router: Router,
    private toastr: NotifyMessageService,
  ) { }
  jobPosts: any;
  carouselOptions = {
    loop: false,
    rewind: true,
    mouseDrag: true,
    touchDrag: true,
    pullDrag: false,
    dots: false,
    navSpeed: 700,
    margin: 10,
    autoplay: true,
    autoplayTimeout: 2000,
    responsive: {
      0: { items: 1 },
      600: { items: 2 },
      1000: { items: 3 }
    },
    nav: true
  };

  ngOnInit(): void {
    this.getData();
  }
  getData() {
    this.homeService.getData().pipe(take(1)).subscribe({
      next: (response) => {
        let data = response.data;
        this.jobPosts = data.jobSalary.content.map((item: any) => ({
          id: item.id,
          title: item.title,
          address: item.address, // Địa chỉ
          image: "assets/web_css/img/post.png", // Hình ảnh mặc định
          link: `/single/${item.id}`, // Đường link chi tiết
          description: item.description,
          salary: item.salary,
          type: item.time
        }));
        this.relatedJobs = data.jobSoon.content.map((item: any) => ({
          id: item.id,
          title: item.title,
          address: item.address,
          image: "assets/web_css/img/p1.png",
          link: item.link,
          description: item.description,
          salary: item.salary,
          type: item.time
        }));
      },
      error: (error) => {
        console.error('Error fetching data:', error);
      }
    })
  }
  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
  }
}
