import { CommonModule } from '@angular/common';
import { Component, effect, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { take } from 'rxjs';
import { HomeService } from '../../services/home.service';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';
import { DownloadAreaComponent } from '../../../../shared/components/download-area/download-area.component';
import { JobCardComponent } from '../../../../shared/components/job-card/job-card.component';
import { SearchFormComponent } from '../../../../shared/components/search-form/search-form.component';
import { JobCardModel } from '../../../../shared/models/jobs/job-card.model';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { SkeletonJobCardComponent } from '../../../../shared/components/skeleton-job-card/skeleton-job-card.component';

@Component({
  selector: 'app-home',
  imports: [
    CommonModule,
    RouterModule,
    SearchFormComponent,
    JobCardComponent,
    SkeletonJobCardComponent,
    CallToActionComponent,
    DownloadAreaComponent,
    TranslatePipe,
  ],
  standalone: true,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
  jobPosts: JobCardModel[] = [];
  isLoading = false;
  constructor(private homeService: HomeService) {
    effect(() => {
      this.jobPosts = this.homeService.jobPosts();
      this.isLoading = this.homeService.isLoading();
    })
  }

  ngOnInit(): void {
    this.homeService.getData();
  }

  trackById(index: number, item: JobCardModel): string | number {
    return item.id;
  }
}
