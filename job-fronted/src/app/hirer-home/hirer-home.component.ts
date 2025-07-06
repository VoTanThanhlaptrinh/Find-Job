import {Component, OnInit} from '@angular/core';
import {RouterLink} from '@angular/router';
import {CarouselModule} from 'ngx-owl-carousel-o';
import {NgForOf, NgIf} from '@angular/common';
import {JobServiceService} from '../services/job-service.service';
import {NotifyMessageService} from '../services/notify-message.service';
import {MatPaginator, PageEvent} from '@angular/material/paginator';

@Component({
  selector: 'app-hirer-home',
  imports: [
    CarouselModule,
    NgForOf,
    NgIf,
    MatPaginator
  ],
  templateUrl: './hirer-home.component.html',
  styleUrl: './hirer-home.component.css'
})
export class HirerHomeComponent implements OnInit{
  jobPosts: any;
  pageIndex: number = 0;
  pageSize: number = 10;
  length : number = 0;
  constructor(private jobService: JobServiceService,
              private notify: NotifyMessageService) {
  }
  ngOnInit(): void {
      this.getJobPost(this.pageIndex,this.pageSize)
      this.countHirerJobPost()
  }
  getJobPost(pageIndex:number, pageSize:number){
    this.jobService.getHirerJobPost(pageIndex,pageSize).subscribe({
      next: res =>{
        this.jobPosts = res.data.content
      },error: err =>{
        this.notify.showMessage(err?.error?.message || 'Lỗi không xác định','', 'error')
      }
    })
  }
  trackById(index: number, item: any): any {
    return item.id;
  }
  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
  }

  handlePage($event: PageEvent) {
    this.getJobPost($event.pageIndex,$event.pageSize);
  }
  countHirerJobPost(){
    this.jobService.countHirerJobPost().subscribe({
      next: res =>{
        this.length = res.data
      },
      error: err => {
        this.notify.showMessage(err?.error?.message || 'Lỗi không xác định','', 'error')
      }
    })
  }
}
