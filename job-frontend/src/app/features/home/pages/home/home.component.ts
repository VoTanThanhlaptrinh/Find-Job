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
export class HomeComponent {
  jobPosts: JobCardModel[] = [];
  constructor(private homeService: HomeService) {
    this.homeService.getData();
    this.jobPosts = this.homeService.jobPosts();
  }

  trackById(index: number, item: JobCardModel): string | number {
    return item.id;
  }
}
