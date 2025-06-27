import { Component, OnInit } from '@angular/core';
import { CategoryService } from '../services/category.service';
import {MatPaginatorModule, PageEvent} from '@angular/material/paginator';
import {take} from 'rxjs';
import {FormsModule} from '@angular/forms';
import {NgxSliderModule, Options} from '@angular-slider/ngx-slider';
import {MatSlider, MatSliderRangeThumb} from '@angular/material/slider';

@Component({
  selector: 'app-category',
  imports: [
    MatPaginatorModule,
    FormsModule,
    NgxSliderModule,
    MatSlider,
    MatSliderRangeThumb
  ],
  standalone: true,
  templateUrl: './category.component.html',
  styleUrl: './category.component.css'
})
export class CategoryComponent implements OnInit {
  listJobsNewest : any;
  addressCount: any;
  pageIndex: number = 0;
  pageSize: number = 10;
  length : number = 0;
  min = 0;
  max = 100000000;
  step = 500000;
  minGap = 2000000;

  minSalary = 5000000;
  maxSalary = 50000000;
  selectedAddresses: Set<string> = new Set<string>();
  selectedTypes: Set<string> = new Set<string>();
  jobTypes = [
    { label: 'Toàn thời gian (Full time)', checked: false },
    { label: 'Bán thời gian (Part time)', checked: false },
    { label: 'Tự do (Freelance)', checked: false },
    { label: 'Thực tập (Internship)', checked: false },
  ];

  constructor(private category: CategoryService) { }
  ngOnInit(): void {
    this.getListJobsNewest(this.pageIndex,this.pageSize);
    this.getAmount();
    this.getAddressCount();
  }
  getListJobsNewest(pageIndex:number, pageSize:number) {
    this.category.listJobsNewest(pageIndex, pageSize).subscribe({
      next: (res) => {
        this.listJobsNewest = res.data.content.map((item: any) => ({
            id: item.id,
            title: item.title,
            address: item.address,
            image: "assets/web_css/img/post.png",
            link: item.link,
            description: item.description,
            salary: item.salary,
            type: item.time
          }));
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      }
    });
  }
  getAmount(){
    this.category.getAmount().pipe(take(1)).subscribe(
      {next :(response) =>{
          this.length  = response.data;
        },
        error: (error) => {
          console.error('Error fetching jobs:', error);
        }
      }
    );
  }

  handlePage($event: PageEvent) {
    this.getListJobsNewest($event.pageIndex,$event.pageSize);
  }
  getAddressCount(){
    this.category.getAddressCount().subscribe({
      next: (res) => {
        this.addressCount = res.data.map((item: any) => ({
          amount: item.count,
          address: item.address,
        }));
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      }
    });
  }
  formatMoney(val: number): string {
    return val.toLocaleString('vi-VN') + '₫';
  }

  onMinChange(value: number) {
    this.minSalary = Math.min(value, this.maxSalary - this.minGap);
    const filter = {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      min: this.minSalary,
      max: this.maxSalary,
      address: Array.from(this.selectedAddresses),
      times: Array.from(this.selectedTypes)
    };
    this.searchWithFilters(filter);
  }

  onMaxChange(value: number) {
    this.maxSalary = Math.max(value, this.minSalary + this.minGap);
    const filter = {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      min: this.minSalary,
      max: this.maxSalary,
      address: Array.from(this.selectedAddresses),
      times: Array.from(this.selectedTypes)
    };
    this.searchWithFilters(filter);
  }

  onFilterChange($event: Event) {
    const input = $event.target as HTMLInputElement;
    const value = input.value;
    const checked = input.checked;

    // Xử lý cho địa chỉ
    if (this.addressCount.find((a: { address: string; }) => a.address === value)) {
      if (checked) {
        this.selectedAddresses.add(value);
      } else {
         this.selectedAddresses.delete(value);
      }
    }

    // Xử lý cho loại công việc
    if (this.jobTypes.find(j => j.label === value)) {
      if (checked) {
        this.selectedTypes.add(value);
      } else {
        this.selectedTypes.delete( value);
      }
    }
    this.pageIndex = 0;
    const filter = {
      pageIndex: this.pageIndex,
      pageSize: this.pageSize,
      min: this.minSalary,
      max: this.maxSalary,
      address: Array.from(this.selectedAddresses),
      times: Array.from(this.selectedTypes)
    };
    this.searchWithFilters(filter);
  }

  private searchWithFilters(filter: any) {
    this.category.filterWithAddressTimeSalary(filter).subscribe({
      next: (res) => {
        this.listJobsNewest = res.data.content.map((item: any) => ({
          id: item.id,
          title: item.title,
          address: item.address,
          image: "assets/web_css/img/post.png",
          link: item.link,
          description: item.description,
          salary: item.salary,
          type: item.time
        }));
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      }
    });
  }
}
