import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { take } from 'rxjs';
import { HomeService } from '../../services/home.service';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { DownloadAreaComponent } from '../../../../shared/components/download-area/download-area.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SearchFormComponent } from '../../../../shared/components/search-form/search-form.component';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';

@Component({
  selector: 'app-home',
  imports: [
    CommonModule,
    RouterModule,
    SearchFormComponent,
    JobCardComponent,
    CallToActionComponent,
    DownloadAreaComponent,
  ],
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
  jobPosts: JobCardModel[] = [];
  relatedJobs: JobCardModel[] = [];

  constructor(private homeService: HomeService) {}

  ngOnInit(): void {
    this.getData();
  }

  getData(): void {
    this.homeService
      .getData()
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.jobPosts = response.data.jobSalary.content;
          this.relatedJobs = response.data.jobSoon.content;
        },
        error: (error) => {
          console.error('Error fetching data:', error);
        },
      });
  }

  trackById(index: number, item: JobCardModel): string | number {
    return item.id;
  }
}
