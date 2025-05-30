import { Component, OnInit } from '@angular/core';
import { CategoryService } from '../services/category.service';
import { get } from 'jquery';

@Component({
  selector: 'app-category',
  imports: [],
  standalone: true,
  templateUrl: './category.component.html',
  styleUrl: './category.component.css'
})
export class CategoryComponent implements OnInit {
  listJobsNewest : any;
  constructor(private category: CategoryService) { }
  ngOnInit(): void {
    this.getListJobsNewest();
  }
  getListJobsNewest() {
    this.category.listJobsNewest().subscribe({
      next: (res) => {
        this.listJobsNewest = res.data.content.map((item: any) => ({
            id: item.id,
            title: item.title,
            address: item.address,
            image: "assets/web_css/img/post.png",
            link: item.link,
            description: item.description,
            salary: item.salary,
            type: item.type
          }));
      },
      error: (error) => {
        console.error('Error fetching jobs:', error);
      }
    });
  }
}
