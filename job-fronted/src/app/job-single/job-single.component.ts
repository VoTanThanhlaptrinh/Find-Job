import { Component, OnInit } from '@angular/core';
import { JobServiceService } from '../services/job-service.service';
import { ActivatedRoute, Route, Router, RouterModule } from '@angular/router';
import { CarouselModule } from 'ngx-owl-carousel-o';
import { HomeService } from '../services/home.service';
import {take} from 'rxjs';
import {NgIf} from '@angular/common';


@Component({
  selector: 'app-job-single',
  imports: [CarouselModule, RouterModule],
  templateUrl: './job-single.component.html',
  styleUrl: './job-single.component.css'
})
export class JobSingleComponent implements OnInit {
  jobId!: string
  jobDetail!: any;
  relatedJobs: any;
  constructor(private jobSerivce: JobServiceService,
    private route: ActivatedRoute,
    private router: Router,
    private homeService: HomeService
  ) {}
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
    this.route.params.subscribe(params => {
      this.jobId = params['id']; // Lấy ID từ URL
       this.getDetailJob(this.jobId);
    });
    this.getRelatedJobs();
  }
  getDetailJob(id: string) {
    this.jobSerivce.getDetailJob(id).pipe(take(1)).subscribe({
      next: (response: any) => {
        this.jobDetail = response.data;
      },
      error: (error) => {
        console.error('Error fetching job details:', error);
        this.router.navigate(['/']) // Redirect to home if error occurs
      }
    });
  }
  getRelatedJobs() {
  this.homeService.getData().pipe(take(1)).subscribe({
      next: (response) => {
         this.relatedJobs = response.data.jobSoon.content.map((item: any) => ({
          id: item.id,
          title: item.title,
          address: item.address,
          image: "assets/web_css/img/r1.png",
          link: item.link,
          description: item.description,
          salary: item.salary,
          type: item.time,
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
