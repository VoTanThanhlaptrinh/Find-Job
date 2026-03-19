import {Component, OnInit} from '@angular/core';
import {JobServiceService} from '../../../jobs/services/job-service.service';
import {NotifyMessageService} from '../../../../core/services/notify-message.service';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import { HirerJobViewModel } from '../../../../shared/models/jobs/job-api-response.model';

@Component({
  selector: 'app-hirer-home',
  imports: [
    
    MatPaginator
  ],
  templateUrl: './hirer-home.component.html',
  styleUrl: './hirer-home.component.css'
})
export class HirerHomeComponent implements OnInit{
  jobPosts: HirerJobViewModel[] = [];
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
  trackById(index: number, item: HirerJobViewModel): string | number {
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
